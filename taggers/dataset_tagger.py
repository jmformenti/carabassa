import argparse
from concurrent.futures import Future, ThreadPoolExecutor, TimeoutError as FuturesTimeoutError
import logging
import os
import sys
from pathlib import Path
from typing import List
import cv2
from tqdm import tqdm

# Add current directory to path to import dataset_api_service
sys.path.append(str(Path(__file__).parent))
from dataset_api_service import ApiException, DatasetApiService, Tag

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s',
    datefmt='%Y-%m-%d %H:%M:%S',
    stream=sys.stdout
)
logger = logging.getLogger(__name__)

class DatasetTagger:
    """
    Base class for creating taggers that process Carabassa datasets and add tags to images.
    """
    def __init__(self, description: str):
        self.parser = argparse.ArgumentParser(description=description)
        self.parser.add_argument("--dataset", type=str, required=True, help="Name of the Carabassa dataset to process")
        self.parser.add_argument("--api-url", type=str, default=os.environ.get("CARABASSA_API_URL", "http://localhost:8080/api/"), help="Carabassa API URL")
        
        # Allow subclasses to add more arguments
        self.add_custom_args(self.parser)
        
        self.args = None
        self.service = None
        self.dataset_id = None

    def add_custom_args(self, parser: argparse.ArgumentParser):
        """
        Override this method to add custom arguments to the parser.
        """
        pass

    def load_image(self, path: Path):
        """
        Helper method to load an image from a path using cv2.
        """
        img = cv2.imread(str(path))
        if img is None:
            return None
        return cv2.cvtColor(img, cv2.COLOR_BGR2RGB)

    def setup(self):
        """
        Override this method to perform any setup before processing items.
        Return False to abort execution.
        """
        return True

    def process_item(self, item, img) -> List[Tag]:
        """
        Override this method to process a single item.
        Return a list of tags to add to the item.
        """
        raise NotImplementedError("Subclasses must implement process_item")

    def post_process(self):
        """
        Optional hook called once after processing all items.
        """
        pass

    def run(self):
        """
        Main execution method.
        """
        self.args = self.parser.parse_args()
        logger.info(f"Processing dataset '{self.args.dataset}' with API URL '{self.args.api_url}'")
        token = os.environ.get("CARABASSA_TOKEN")
        self.service = DatasetApiService(self.args.api_url, token=token)

        try:
            self.dataset_id = self.service.find_by_name(self.args.dataset)
        except Exception as e:
            logger.error(f"Error finding dataset '{self.args.dataset}': {e}")
            return

        if not self.setup():
            return
        self._process_dataset_items()
        self.post_process()

    def get_search_query(self) -> str:
        """
        Return the search query string to filter items.
        Default is 'type:I' to find only images.
        """
        return "type:I"

    def _ensure_tag_infos(self, tag_names, tag_info_meta=None) -> bool:
        tag_info_meta = tag_info_meta or {}
        for tag_name in tag_names:
            meta = tag_info_meta.get(tag_name, {})
            try:
                self.service.create_tag_info(
                    tag_name,
                    description=meta.get("description"),
                    alias=meta.get("alias"),
                    internal=meta.get("internal"),
                    sortable=meta.get("sortable"),
                    tag_type=meta.get("type"),
                )
                logger.info("Created tag info for '%s'.", tag_name)
            except ApiException as exc:
                if exc.status_code == 409:
                    logger.info("Tag info '%s' already exists.", tag_name)
                    continue
                logger.error("Failed to create tag info '%s': %s", tag_name, exc)
                return False
        return True

    def _process_dataset_items(self):
        logger.info(f"Fetching items info for dataset ID {self.dataset_id}...")
        
        BATCH_SIZE = 100
        search_query = self.get_search_query()
        enable_prefetch = "missing_tag:" not in search_query.lower()
        if enable_prefetch:
            self._process_with_paged_prefetch(search_query, BATCH_SIZE)
        else:
            logger.info("Prefetch disabled for mutable query: %s", search_query)
            self._process_with_mutable_first_page(search_query, BATCH_SIZE)

    def _process_with_mutable_first_page(self, search_query, batch_size):
        first_page = self._fetch_page_sync(search_query, 0, batch_size)
        total_items = first_page.page.totalElements
        if total_items == 0:
            logger.info("No images found.")
            return

        logger.info(f"Found {total_items} images. Starting processing...")
        previous_page_item_ids = None
        stagnant_rounds = 0
        with tqdm(total=total_items, desc="Processing items", unit="img", file=sys.stderr) as pbar:
            while True:
                page_zero = self._fetch_page_sync(search_query, 0, batch_size)
                items = page_zero.content
                if not items:
                    break

                # Protection if some items fails to update
                current_page_item_ids = tuple(item.id for item in items)
                if current_page_item_ids == previous_page_item_ids:
                    stagnant_rounds += 1
                    if stagnant_rounds >= 3:
                        logger.warning(
                            "Stopping mutable page-0 loop: first page did not change after %s rounds.",
                            stagnant_rounds,
                        )
                        break
                else:
                    stagnant_rounds = 0
                    previous_page_item_ids = current_page_item_ids

                for item in items:
                    self._handle_single_item(item, pbar)
                    pbar.update(1)

    def _process_with_paged_prefetch(self, search_query, batch_size):
        first_result = self._fetch_page_sync(search_query, 0, batch_size)
        total_items = first_result.page.totalElements
        total_pages = first_result.page.totalPages
        if total_items == 0:
            logger.info("No images found.")
            return

        logger.info(f"Found {total_items} images. Starting processing...")
        with tqdm(total=total_items, desc="Processing items", unit="img", file=sys.stderr) as pbar, ThreadPoolExecutor(max_workers=1) as executor:
            current_page = 0
            items = first_result.content
            next_page_future: Future | None = None
            if total_pages > 1:
                next_page_future = self._prefetch_page_async(search_query, current_page + 1, executor, batch_size)

            while True:
                if not items and current_page > 0:
                    break
                for item in items:
                    self._handle_single_item(item, pbar)
                    pbar.update(1)
                if current_page >= total_pages - 1:
                    break

                current_page += 1
                paged_result = self._next_page_with_fallback(next_page_future, search_query, current_page, batch_size)
                items = paged_result.content

                if current_page < total_pages - 1:
                    next_page_future = self._prefetch_page_async(search_query, current_page + 1, executor, batch_size)
                else:
                    next_page_future = None

    def _next_page_with_fallback(self, next_page_future, search_query, current_page, batch_size):
        if next_page_future is None:
            return self._fetch_page_sync(search_query, current_page, batch_size)
        try:
            return next_page_future.result(timeout=30)
        except FuturesTimeoutError:
            next_page_future.cancel()
            logger.warning("Timeout prefetching page %s. Falling back to synchronous fetch.", current_page)
            return self._fetch_page_sync(search_query, current_page, batch_size)
        except Exception as e:
            logger.warning("Error prefetching page %s (%s). Falling back to synchronous fetch.", current_page, e)
            return self._fetch_page_sync(search_query, current_page, batch_size)

    def _prefetch_page_async(self, search_query, page, executor, batch_size):
        return executor.submit(
            self._fetch_page_sync,
            search_query,
            page,
            batch_size,
        )

    def _fetch_page_sync(self, search_query, page, batch_size):
        result = self.service.find_items(
            self.dataset_id,
            search_string=search_query,
            page=page,
            size=batch_size,
        )
        return result

    def add_item_tag(self, item_id, tag):
        result = self.service.add_item_tag(self.dataset_id, item_id, tag)
        return result

    def _handle_single_item(self, item, pbar):
        try:
            # Download item to cache
            img_path = self.service.get_item_content(self.dataset_id, item.id)
            
            img = self.load_image(img_path)
            if img is None:
                tqdm.write(f"⚠ No image for item {item.id} found at: {img_path}", file=sys.stderr)

            tags = self.process_item(item, img)

            if tags:
                for tag in tags:
                    try:
                        self.add_item_tag(item.id, tag)
                    except Exception as e:
                        tqdm.write(f"Failed to add tag to item {item.filename} ({item.id}): {e}", file=sys.stderr)
        
        except Exception as e:
            tqdm.write(f"Error processing item {item.id} ({item.filename}): {e}", file=sys.stderr)
