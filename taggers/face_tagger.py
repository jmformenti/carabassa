from uniface import ArcFace, RetinaFace
from pathlib import Path
import numpy as np
import chromadb
from chromadb.config import Settings
from typing import List, Optional
import hashlib
import onnxruntime
import logging
from tqdm import tqdm
import sys
import shutil

# Add current directory to path to import dataset_tool
sys.path.append(str(Path(__file__).parent))
from dataset_tagger import DatasetTagger
from dataset_api_service import Tag, BoundingBox

logger = logging.getLogger(__name__)



SOURCE_TAG_NAME = "tagger.face.reference"
TAG_NAME = "tagger.face.person"
TAGGER_TAG_NAME = "tagger.face.processed"
TAG_INFO_META = {
    TAG_NAME: {
        "description": "Detected person name from face recognition.",
        "alias": "person",
        "internal": False,
        "sortable": False,
        "showInHelp": True,
        "type": "STRING",
    },
    SOURCE_TAG_NAME: {
        "description": "Reference face tag used to build the recognition database.",
        "alias": "face.reference",
        "internal": False,
        "sortable": False,
        "showInHelp": False,
        "type": "STRING",
    },
    TAGGER_TAG_NAME: {
        "description": "Marker tag indicating the item was processed by the face tagger.",
        "alias": None,
        "internal": True,
        "sortable": False,
        "showInHelp": False,
        "type": "BOOLEAN",
    },
}

class FaceDatabase:
    """Face database using ChromaDB"""
    
    def __init__(self, db_path: str = "./chroma_db"):
        self.client = chromadb.PersistentClient(
            path=db_path,
            settings=Settings(anonymized_telemetry=False)
        )
        
        # IMPORTANT: We specify "cosine" because default is L2
        self.faces_collection = self.client.get_or_create_collection(
            name="faces",
            metadata={"hnsw:space": "cosine"}
        )
    
    def _get_file_hash(self, file_path: Path) -> str:
        """Calculate file hash"""
        with open(file_path, 'rb') as f:
            return hashlib.md5(f.read()).hexdigest()
    
    def add_face(self, file_path: Path, embedding: np.ndarray, person_name: str = "Unknown", tag_id: Optional[int] = None):
        """Add a face to the database"""
        file_hash = self._get_file_hash(file_path)
        doc_id = f"face_{file_hash}_{tag_id}" if tag_id is not None else f"face_{file_hash}"
        
        # Check if already exists
        existing = self.faces_collection.get(ids=[doc_id])
        if existing['ids']:
            if logger.isEnabledFor(logging.DEBUG):
                tqdm.write(f"✓ {file_path.name} ({person_name}) already exists in DB.")
        else:
            # Add new embedding
            self.faces_collection.add(
                embeddings=[embedding.flatten().tolist()],
                documents=[str(file_path)],
                metadatas=[{
                "person_name": person_name,
                "filename": file_path.name,
                "path": str(file_path),
                "hash": file_hash
                }],
                ids=[doc_id]
            )
            tqdm.write(f"→ Added to DB: {person_name} ({file_path.name})")
    
    def get_faces(self) -> List[np.ndarray]:
        """Retrieve all faces"""
        results = self.faces_collection.get(include=["embeddings"])
        if results['embeddings'] is None or len(results['embeddings']) == 0:
            return []
        return [np.array(emb) for emb in results['embeddings']]
    
    def search_similar_faces(self, query_embedding: np.ndarray, n_results: int = 5, threshold: float = 0.45):
        """Search for similar faces in the database"""
        if self.faces_collection.count() == 0:
            return []
        
        results = self.faces_collection.query(
            query_embeddings=[query_embedding.flatten().tolist()],
            n_results=min(n_results, self.faces_collection.count())
        )
        
        matches = []
        if results['distances'] and results['distances'][0]:
            for i, (distance, metadata) in enumerate(zip(results['distances'][0], results['metadatas'][0])):
                # ChromaDB returns distance (lower = more similar)
                similarity = 1 - distance
                
                if similarity >= threshold:
                    matches.append({
                        'similarity': similarity,
                        'person_name': metadata.get('person_name', 'Unknown'),
                        'filename': metadata['filename'],
                        'path': metadata['path']
                    })
        
        return matches

class FaceTagger(DatasetTagger):
    def __init__(self):
        super().__init__(description="Face Recognition")
        self.detector = None
        self.recognizer = None
        self.db = None

    def _calculate_iou(self, boxA, boxB):
        """Calculate Intersection over Union (IoU) between two bounding boxes"""
        # box = [x1, y1, x2, y2]
        xA = max(boxA[0], boxB[0])
        yA = max(boxA[1], boxB[1])
        xB = min(boxA[2], boxB[2])
        yB = min(boxA[3], boxB[3])
        interArea = max(0, xB - xA + 1) * max(0, yB - yA + 1)
        boxAArea = (boxA[2] - boxA[0] + 1) * (boxA[3] - boxA[1] + 1)
        boxBArea = (boxB[2] - boxB[0] + 1) * (boxB[3] - boxB[1] + 1)
        return interArea / float(boxAArea + boxBArea - interArea)

    def add_custom_args(self, parser):
        parser.add_argument("--threshold", type=float, default=0.45, help="Similarity threshold (default: 0.45)")
        parser.add_argument("--force", action="store_true", help="Force reprocessing of all items")
        parser.add_argument("--global-faces", action="store_true", help="Use a shared face DB across all datasets")
        parser.add_argument("--rebuild-db", action="store_true", help="Rebuild face DB for this run")
        parser.add_argument("--device", type=str, choices=["cuda", "cpu"], default="cuda", help="Device to run inference on")
        parser.add_argument("--list", action="store_true", help="List all faces in the database")

    def get_search_query(self) -> str:
        if self.args.force:
             return "type:I"
        return f"type:I missing_tag:{TAGGER_TAG_NAME}"

    def handle_test_mode(self):
        if self.args.list:
            self.list_db_faces()
            return True
        return False

    def list_db_faces(self):
        db_path = self._resolve_db_path()
        if not db_path.exists():
            print(f"Database at '{db_path}' does not exist.")
            return
        
        db = FaceDatabase(str(db_path))
        results = db.faces_collection.get()
        
        print(f"\nDatabase: {db_path}")
        print(f"Found {len(results['ids'])} faces:\n")
        print(f"{'Person':<20} | {'File'}")
        print("-" * 50)
        
        for i in range(len(results['ids'])):
            metadata = results['metadatas'][i]
            person = metadata.get('person_name', 'Unknown')
            filename = metadata.get('filename', 'Unknown')
            print(f"{person:<20} | {filename}")
        print("")

    def setup(self):
        if not self._ensure_tag_infos(
            (TAG_NAME, SOURCE_TAG_NAME, TAGGER_TAG_NAME),
            TAG_INFO_META,
        ):
            return False

        if self.args.device == "cuda":
            try:
                onnxruntime.preload_dlls(cuda=True, cudnn=True)
            except Exception:
                pass
            providers = ["CUDAExecutionProvider", "CPUExecutionProvider"]
        else:
            providers = ["CPUExecutionProvider"]

        logger.info(f"Using device: {self.args.device}")

        self.detector = RetinaFace()
        self.recognizer = ArcFace()
        db_path = self._resolve_db_path()
        if self.args.rebuild_db:
            shutil.rmtree(db_path, ignore_errors=True)
        logger.info("Using face DB at '%s'.", db_path)
        self.db = FaceDatabase(str(db_path))

        return self._process_reference_faces()

    def _resolve_db_path(self) -> Path:
        base_dir = Path("./chroma_db")
        if self.args.global_faces:
            return base_dir / "global_faces"
        safe_name = self.args.dataset.replace("/", "_").replace("\\", "_").replace(" ", "_")
        return base_dir / f"faces_{safe_name}"

    def _get_reference_datasets(self) -> list[tuple[int, str]]:
        if self.args.global_faces:
            datasets = self.service.find_all()
            return [(d.id, d.name) for d in datasets if d.id is not None]
        return [(self.dataset_id, self.args.dataset)]

    def _process_reference_faces(self):
        datasets = self._get_reference_datasets()
        for dataset_id, dataset_name in datasets:
            if dataset_id is None:
                continue
            logger.info(f"Processing faces from dataset '{dataset_name}' tagged with '{SOURCE_TAG_NAME}'...")
            reference_tags = self.service.find_dataset_item_tags_by_name(dataset_id, SOURCE_TAG_NAME)
            self._process_dataset_faces(dataset_id, reference_tags)
        return True

    def _process_dataset_faces(self, dataset_id, reference_tags):
        if not reference_tags:
            logger.info("No source faces found in dataset.")
            return True

        # Group by item_id
        item_tags = {}
        for info in reference_tags:
            if info.item_id not in item_tags:
                item_tags[info.item_id] = []
            item_tags[info.item_id].append(info)

        for item_id, infos in tqdm(item_tags.items(), desc="Extracting facial encodings"):
            try:
                # Get full item to have the tags with bounding boxes
                item = self.service.find_item(dataset_id, item_id)
                img_path = self.service.get_item_content(dataset_id, item_id)
                img = self.load_image(img_path)
                
                if img is None: continue

                # Detect all faces once for this image to get landmarks
                detected_faces = self.detector.detect(img)

                for tag_info in infos:
                    person_name = str(tag_info.tag_value)
                    
                    # Find the actual tag representation in the item to get boundingBox
                    # We match by tag_id for precision, especially if there are multiple tags for the same person
                    source_tag = next((t for t in (item.tags or []) if t.get('id') == tag_info.tag_id), None)
                    if not source_tag or 'boundingBox' not in source_tag:
                        logger.warning(f"No boundingBox for tag '{person_name}' (ID: {tag_info.tag_id}) in item {item_id}")
                        continue
                    
                    bb = source_tag['boundingBox']
                    # boundingBox is [minX, minY, width, height]
                    # Convert to [x1, y1, x2, y2]
                    tag_box = [bb['minX'], bb['minY'], bb['minX'] + bb['width'], bb['minY'] + bb['height']]

                    # Find best match from detected faces
                    best_face = None
                    max_iou = 0
                    for face in detected_faces:
                        iou = self._calculate_iou(tag_box, face.bbox)
                        if iou > max_iou:
                            max_iou = iou
                            best_face = face
                    
                    if best_face and max_iou > 0.5:
                        emb = self.recognizer.get_normalized_embedding(img, best_face.landmarks)
                        self.db.add_face(img_path, emb, person_name, tag_info.tag_id)
                    else:
                        tqdm.write(f"Could not match tag '{person_name}' to any detected face ({len(detected_faces)}) in item {item_id} (max IoU: {max_iou:.2f}): tag box ({tag_box} vs face box ({face.bbox}))")
            
            except Exception as e:
                logger.error(f"Error processing facial encoding for item {item_id}: {e}")

        return True

    def process_item(self, item, img) -> List[Tag]:
        tags = []
        if img is not None:
            try:
                faces = self.detector.detect(img)
                
                for face in faces:
                    emb = self.recognizer.get_normalized_embedding(img, face.landmarks)
                    matches_found = self.db.search_similar_faces(emb, threshold=self.args.threshold)

                    if matches_found:
                        best_match = matches_found[0]
                        person_name = best_match['person_name']
                        similarity = best_match['similarity']
                        
                        # Log match
                        tqdm.write(f"✓ Match found in {item.filename}: {person_name} ({similarity:.2f})")
                        
                        # Convert bbox [x1, y1, x2, y2] to [minX, minY, width, height]
                        x1, y1, x2, y2 = face.bbox
                        width = int(x2 - x1)
                        height = int(y2 - y1)
                        
                        bbox = BoundingBox(
                            minX=int(x1),
                            minY=int(y1),
                            width=width,
                            height=height
                        )
                        
                        tag = Tag(
                            name=TAG_NAME,
                            value=person_name,
                            boundingBox=bbox
                        )
                        tags.append(tag)
                        
            except Exception as e:
                tqdm.write(f"Error analyzing faces for item {item.id}: {e}")
             
        # Add tag to mark item as processed
        tags.append(Tag(name=TAGGER_TAG_NAME, value=True))
             
        return tags

if __name__ == "__main__":
    tool = FaceTagger()
    tool.run()
    logger.info("done.")
