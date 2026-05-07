import logging
import os
import sys
import imagehash
import vptree
from dataclasses import dataclass, field
from concurrent.futures import ProcessPoolExecutor, as_completed

from pathlib import Path
from PIL import Image
from tqdm import tqdm
from collections import defaultdict

# Add current directory to path to import dataset_tool
sys.path.append(str(Path(__file__).parent))

from dataset_api_service import Tag
from dataset_tagger import DatasetTagger


logger = logging.getLogger(__name__)

TAGGER_TAG_NAME = "tagger.duplicate.processed"
PHASH_TAG_NAME = "tagger.duplicate.phash"
DUPLICATED_TAG_NAME = "tagger.duplicate.duplicated"
DUPLICATED_GROUP_TAG_NAME = "tagger.duplicate.group"
TAG_INFO_META = {
    TAGGER_TAG_NAME: {
        "description": "Marker tag indicating the item was processed by the duplicate tagger.",
        "alias": None,
        "internal": True,
        "sortable": False,
        "showInHelp": False,
        "type": "BOOLEAN",
    },
    PHASH_TAG_NAME: {
        "description": "Perceptual hash for duplicate detection.",
        "alias": "phash",
        "internal": True,
        "sortable": False,
        "showInHelp": False,
        "type": "STRING",
    },
    DUPLICATED_TAG_NAME: {
        "description": "Flag indicating the item is part of a duplicate group.",
        "alias": "duplicated",
        "internal": False,
        "sortable": False,
        "showInHelp": True,
        "type": "BOOLEAN",
    },
    DUPLICATED_GROUP_TAG_NAME: {
        "description": "Group identifier for duplicate items.",
        "alias": "duplicated.group",
        "internal": False,
        "sortable": True,
        "showInHelp": True,
        "type": "STRING",
    },
}

DEFAULT_THRESHOLD = 12
DEFAULT_WORKERS = 1
DEFAULT_NEIGHBOR_CHUNK_SIZE = 500


_WORKER_TREE = None
_WORKER_ITEMS_BY_ID = {}
_WORKER_THRESHOLD = 0


@dataclass
class HashedItem:
    item_id: int
    phash: imagehash.ImageHash
    has_duplicated_tag: bool = False
    duplicated_tag_ids: list[int] = field(default_factory=list)
    duplicated_group_tags: list["DuplicatedTag"] = field(default_factory=list)


@dataclass
class DuplicatedTag:
    id: int
    value: str

@dataclass
class UnionFind:
    def __init__(self, items):
        self.parent = {item.item_id: item.item_id for item in items}
        self.rank = {item.item_id: 0 for item in items}
    
    def find(self, i):
        root = i
        while self.parent[root] != root:
            root = self.parent[root]

        # Path compression
        while self.parent[i] != i:
            parent = self.parent[i]
            self.parent[i] = root
            i = parent
        return root

    def union(self, i, j):
        root_i = self.find(i)
        root_j = self.find(j)
        if root_i == root_j:
            return

        rank_i = self.rank[root_i]
        rank_j = self.rank[root_j]

        # Union by rank to avoid tall trees
        if rank_i < rank_j:
            self.parent[root_i] = root_j
        elif rank_i > rank_j:
            self.parent[root_j] = root_i
        else:
            self.parent[root_j] = root_i
            self.rank[root_i] += 1


def _distance_func(item1, item2):
    return item1.phash - item2.phash


def _init_neighbors_worker(serialized_items, threshold):
    global _WORKER_TREE, _WORKER_ITEMS_BY_ID, _WORKER_THRESHOLD
    items = [HashedItem(item_id=item_id, phash=imagehash.hex_to_hash(phash)) for item_id, phash in serialized_items]
    _WORKER_ITEMS_BY_ID = {item.item_id: item for item in items}
    _WORKER_TREE = vptree.VPTree(items, _distance_func)
    _WORKER_THRESHOLD = threshold


def _find_neighbors_chunk(item_ids):
    pairs = []
    for item_id in item_ids:
        item = _WORKER_ITEMS_BY_ID.get(item_id)
        if item is None:
            continue
        results = _WORKER_TREE.get_all_in_range(item, _WORKER_THRESHOLD)
        for _, neighbor in results:
            if neighbor.item_id <= item.item_id:
                continue
            pairs.append((item.item_id, neighbor.item_id))
    return pairs, len(item_ids)

class DuplicateTagger(DatasetTagger):
    def __init__(self):
        super().__init__(description="Find similar images in a dataset")

    def add_custom_args(self, parser):
        parser.add_argument(
            "--threshold",
            type=int,
            default=DEFAULT_THRESHOLD,
            help=f"Maximum Hamming distance to consider two images similar (0-5 identical, 10-15 similar] (default: {DEFAULT_THRESHOLD})",
        )
        parser.add_argument(
            "--force",
            action="store_true",
            help="Force reprocessing of all images",
        )
        parser.add_argument(
            "--workers",
            type=int,
            default=DEFAULT_WORKERS,
            help=f"Workers for neighbor search (CPU-bound). 1 = sequential (default: {DEFAULT_WORKERS})",
        )
        parser.add_argument(
            "--neighbor-chunk-size",
            type=int,
            default=DEFAULT_NEIGHBOR_CHUNK_SIZE,
            help=f"Number of unique hashes processed per worker task (default: {DEFAULT_NEIGHBOR_CHUNK_SIZE})",
        )

    def get_search_query(self) -> str:
        if self.args.force:
            return "type:I"
        return f"type:I missing_tag:{TAGGER_TAG_NAME}"

    def setup(self):
        return self._ensure_tag_infos(
            (TAGGER_TAG_NAME, PHASH_TAG_NAME, DUPLICATED_TAG_NAME, DUPLICATED_GROUP_TAG_NAME),
            TAG_INFO_META,
        )

    def process_item(self, item, img):
        tags = []
        if img is not None:
            try:
                phash = imagehash.phash(Image.fromarray(img))
                tags.append(Tag(name=PHASH_TAG_NAME, value=str(phash)))
            except Exception as exc:
                logger.error("Error calculating pHash for item %s (%s): %s", item.id, item.filename, exc)
        tags.append(Tag(name=TAGGER_TAG_NAME, value=True))
        return tags

    def post_process(self):
        self._find_similar_groups()

    def _find_similar_groups(self):
        logger.info("Grouping similar images (threshold=%s)...", self.args.threshold)

        hashed_items = self._load_phash_items_from_api()
        if not hashed_items:
            return

        # Initialize Union-Find with all item IDs
        uf = UnionFind(hashed_items)
        unique_items, items_by_phash = self._build_unique_items(hashed_items)
        logger.info(
            "Loaded %s items, %s unique pHash values.",
            len(hashed_items),
            len(unique_items),
        )

        # Collapse exact duplicates first (same pHash)
        for item_ids in items_by_phash.values():
            if len(item_ids) <= 1:
                continue
            representative_id = item_ids[0]
            for item_id in item_ids[1:]:
                uf.union(representative_id, item_id)

        self._find_neighbors(unique_items, uf)

        # Extract resulting groups
        groups = defaultdict(list)
        for item in hashed_items:
            root = uf.find(item.item_id)
            groups[root].append(item)

        # Filter groups with more than 1 item
        final_groups = self._refine_groups_pairwise(groups.values())

        if not final_groups:
            logger.info("No similar groups found.")
        else:
            logger.info("Found %s distinct groups.", len(final_groups))
            for i, group in enumerate(tqdm(final_groups, desc="Tagging groups", unit="group")):
                group_id = min(str(obj.phash) for obj in group)
                # logger.info(f"Group {i+1} (id:{group_id}) ({len(group)} images)")
                self._apply_duplicate_tags(group, group_id)

        self._remove_duplicate_tags_from_singletons(hashed_items, final_groups)
                
        return final_groups

    def _refine_groups_pairwise(self, groups):
        """
        Split connected components into stricter subgroups.
        Rule: inside each subgroup, every pair must satisfy distance <= threshold.
        This avoids chain effects (A~B, B~C, but A far from C).
        """
        refined_groups = []
        for group in groups:
            if len(group) <= 1:
                continue
            subgroups = self._split_group_pairwise(group, self.args.threshold)
            for subgroup in subgroups:
                if len(subgroup) > 1:
                    refined_groups.append(subgroup)
        return refined_groups

    def _split_group_pairwise(self, group, threshold):
        subgroups = []
        # Deterministic order for reproducibility
        ordered_group = sorted(group, key=lambda x: x.item_id)

        for item in ordered_group:
            placed = False
            for subgroup in subgroups:
                # Complete-link style: item must be close to all members of subgroup
                if all((item.phash - member.phash) <= threshold for member in subgroup):
                    subgroup.append(item)
                    placed = True
                    break
            if not placed:
                subgroups.append([item])
        return subgroups

    def _build_unique_items(self, hashed_items):
        items_by_phash = defaultdict(list)
        representative_by_phash = {}

        for item in hashed_items:
            phash_key = str(item.phash)
            items_by_phash[phash_key].append(item.item_id)
            representative_by_phash.setdefault(phash_key, item)

        unique_items = list(representative_by_phash.values())
        return unique_items, items_by_phash

    def _find_neighbors(self, unique_items, uf):
        workers = max(1, int(self.args.workers))
        if workers == 1:
            tree = vptree.VPTree(unique_items, _distance_func)
            for item in tqdm(unique_items, desc="Finding neighbors", unit="item"):
                results = tree.get_all_in_range(item, self.args.threshold)
                for _, neighbor in results:
                    if neighbor.item_id <= item.item_id:
                        continue
                    uf.union(item.item_id, neighbor.item_id)
            return

        logger.info("Finding neighbors in parallel with %s workers...", workers)
        chunk_size = max(1, int(self.args.neighbor_chunk_size))
        item_ids = [item.item_id for item in unique_items]
        chunks = [item_ids[i:i + chunk_size] for i in range(0, len(item_ids), chunk_size)]
        serialized_items = [(item.item_id, str(item.phash)) for item in unique_items]

        with ProcessPoolExecutor(
            max_workers=workers,
            initializer=_init_neighbors_worker,
            initargs=(serialized_items, self.args.threshold),
        ) as executor, tqdm(total=len(unique_items), desc="Finding neighbors", unit="item") as pbar:
            futures = [executor.submit(_find_neighbors_chunk, chunk) for chunk in chunks]
            for future in as_completed(futures):
                pairs, processed = future.result()
                for item_id_a, item_id_b in pairs:
                    uf.union(item_id_a, item_id_b)
                pbar.update(processed)

    def _apply_duplicate_tags(self, group, group_id):
        for obj in group:
            try:
                if not obj.has_duplicated_tag:
                    self.add_item_tag(obj.item_id, Tag(name=DUPLICATED_TAG_NAME, value=True))

                stale_group_tags = [tag for tag in obj.duplicated_group_tags if tag.value != group_id]

                for duplicated_tag in stale_group_tags:
                    self.service.delete_item_tag(self.dataset_id, obj.item_id, int(duplicated_tag.id))

                has_same_group_tag = any(tag.value == group_id for tag in obj.duplicated_group_tags)
                if not has_same_group_tag:
                    self.add_item_tag(obj.item_id, Tag(name=DUPLICATED_GROUP_TAG_NAME, value=group_id))
            except Exception as exc:
                logger.warning("Failed applying duplicate tags to item %s: %s", obj.item_id, exc)

    def _remove_duplicate_tags_from_singletons(self, hashed_items, final_groups):
        items_in_groups = {obj.item_id for group in final_groups for obj in group}

        for obj in tqdm(hashed_items, desc="Removing orphan groups", unit="item"):
            if obj.item_id in items_in_groups:
                continue

            if not obj.duplicated_tag_ids and not obj.duplicated_group_tags:
                continue

            tqdm.write(f"Removing duplicated tags for item {obj.item_id}..")
            for tag_id in obj.duplicated_tag_ids:
                try:
                    self.service.delete_item_tag(self.dataset_id, obj.item_id, int(tag_id))
                except Exception as exc:
                    tqdm.write("Failed removing duplicated tag from item %s: %s", obj.item_id, exc)

            for duplicated_tag in obj.duplicated_group_tags:
                try:
                    self.service.delete_item_tag(self.dataset_id, obj.item_id, int(duplicated_tag.id))
                except Exception as exc:
                    tqdm.write("Failed removing duplicated.group tag from item %s: %s", obj.item_id, exc)

    def _load_phash_items_from_api(self):
        hashed_items = []
        logger.info("Loading image hashes...")
        tag_rows = self.service.find_dataset_item_tags_by_name(self.dataset_id, PHASH_TAG_NAME, size=1000)
        logger.info(f"Found {len(tag_rows)} images with phash tag.")

        duplicated_rows = self.service.find_dataset_item_tags_by_name(
            self.dataset_id, DUPLICATED_TAG_NAME,
            size=1000,
            show_progress=False
        )
        duplicated_tag_ids_by_item = defaultdict(list)
        for row in duplicated_rows:
            if row.item_id is None or row.tag_id is None:
                continue
            duplicated_tag_ids_by_item[row.item_id].append(int(row.tag_id))
        duplicated_item_ids = set(duplicated_tag_ids_by_item.keys())

        duplicated_group_rows = self.service.find_dataset_item_tags_by_name(
            self.dataset_id,
            DUPLICATED_GROUP_TAG_NAME,
            size=1000,
            show_progress=False
        )
        duplicated_group_tags_by_item = defaultdict(list)
        for row in duplicated_group_rows:
            if row.item_id is None or row.tag_id is None:
                continue
            duplicated_group_tags_by_item[row.item_id].append(
                DuplicatedTag(
                    id=int(row.tag_id),
                    value="" if row.tag_value is None else str(row.tag_value),
                )
            )

        for row in tag_rows:
            try:
                phash_value = row.tag_value
                if not phash_value:
                    tqdm.write(f"Warning: {row.item_id} without phash calculated.")
                    continue
                hashed_items.append(
                    HashedItem(
                        item_id=row.item_id,
                        phash=imagehash.hex_to_hash(str(phash_value)),
                        has_duplicated_tag=row.item_id in duplicated_item_ids,
                        duplicated_tag_ids=list(duplicated_tag_ids_by_item.get(row.item_id, [])),
                        duplicated_group_tags=list(duplicated_group_tags_by_item.get(row.item_id, [])),
                    )
                )
            except Exception as exc:
                logger.warning("Failed loading phash for item %s: %s", row.item_id, exc)

        return hashed_items


if __name__ == "__main__":
    tool = DuplicateTagger()
    tool.run()
    logger.info("done.")
