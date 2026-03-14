from uniface import ArcFace, RetinaFace
from pathlib import Path
import numpy as np
import chromadb
from chromadb.config import Settings
from typing import List
import hashlib
import onnxruntime
import logging
from tqdm import tqdm
import sys

# Add current directory to path to import dataset_tool
sys.path.append(str(Path(__file__).parent))
from dataset_tool import DatasetTool
from dataset_api_service import (
    Tag,
    BoundingBox,
)

logger = logging.getLogger(__name__)

onnxruntime.preload_dlls(cuda=True, cudnn=True)

SOURCE_TAG_NAME = "face.name"
TAG_NAME = "person"
TAGGER_TAG_NAME = "tagger.detect_faces"

def calculate_iou(boxA, boxB):
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
    
    def add_face(self, file_path: Path, embedding: np.ndarray, person_name: str = "Unknown"):
        """Add a face to the database"""
        file_hash = self._get_file_hash(file_path)
        doc_id = f"face_{file_hash}"
        
        # Check if already exists
        existing = self.faces_collection.get(ids=[doc_id])
        if existing['ids']:
            logger.info(f"✓ {file_path.name} ({person_name}) already exists in DB.")
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
            logger.info(f"→ Added to DB: {person_name} ({file_path.name})")
    
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

class DetectFacesTool(DatasetTool):
    def __init__(self):
        super().__init__(description="Carabassa Face Recognition")
        self.detector = None
        self.recognizer = None
        self.db = None

    def add_custom_args(self, parser):
        parser.add_argument("--threshold", type=float, default=0.45, help="Similarity threshold (default: 0.45)")
        parser.add_argument("--force", action="store_true", help="Force reprocessing of all items")

    def get_search_query(self) -> str:
        if self.args.force:
             return "type:I"
        return f"type:I missing_tag:{TAGGER_TAG_NAME}"

    def setup(self):
        self.detector = RetinaFace()
        self.recognizer = ArcFace()
        self.db = FaceDatabase()
        
        return self._process_dataset_faces()

    def _process_dataset_faces(self):
        logger.info(f"Processing faces from dataset '{self.dataset_id}' tagged with '{SOURCE_TAG_NAME}'...")
        
        # 1. Find all items tagged with SOURCE_TAG_NAME
        tag_infos = self.service.find_dataset_item_tags_by_name(self.dataset_id, SOURCE_TAG_NAME)
        
        if not tag_infos:
            logger.info("No source faces found in dataset.")
            return True

        # Group by item_id
        item_tags = {}
        for info in tag_infos:
            if info.item_id not in item_tags:
                item_tags[info.item_id] = []
            item_tags[info.item_id].append(info)

        for item_id, infos in tqdm(item_tags.items(), desc="Extracting facial encodings"):
            try:
                # Get full item to have the tags with bounding boxes
                item = self.service.find_item(self.dataset_id, item_id)
                img_path = self.service.get_item_content(self.dataset_id, item_id)
                img = self.load_image(img_path)
                
                if img is None: continue

                # Detect all faces once for this image to get landmarks
                detected_faces = self.detector.detect(img)

                for tag_info in infos:
                    person_name = str(tag_info.tag_value)
                    
                    # Find the actual tag representation in the item to get boundingBox
                    source_tag = next((t for t in item.tags if t['name'] == SOURCE_TAG_NAME and str(t['value']) == person_name), None)
                    if not source_tag or 'boundingBox' not in source_tag:
                        logger.warning(f"No boundingBox for tag '{person_name}' in item {item_id}")
                        continue
                    
                    bb = source_tag['boundingBox']
                    # boundingBox is [minX, minY, width, height]
                    # Convert to [x1, y1, x2, y2]
                    tag_box = [bb['minX'], bb['minY'], bb['minX'] + bb['width'], bb['minY'] + bb['height']]

                    # Find best match from detected faces
                    best_face = None
                    max_iou = 0
                    for face in detected_faces:
                        iou = calculate_iou(tag_box, face.bbox)
                        if iou > max_iou:
                            max_iou = iou
                            best_face = face
                    
                    if best_face and max_iou > 0.5:
                        emb = self.recognizer.get_normalized_embedding(img, best_face.landmarks)
                        self.db.add_face(img_path, emb, person_name)
                    else:
                        logger.warning(f"Could not match tag '{person_name}' to any detected face in item {item_id} (max IoU: {max_iou:.2f})")
            
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
    tool = DetectFacesTool()
    tool.run()
    logger.info("done.")
