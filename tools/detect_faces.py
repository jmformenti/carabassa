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

TAG_NAME = "person"
TAGGER_TAG_NAME = "tagger.detect_faces"

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
        parser.add_argument("--known-dir", type=str, default="known_faces", help="Directory containing known faces")
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
        
        if not self._process_known_images(self.args.known_dir):
            logger.warning(f"Known faces directory {self.args.known_dir} does not exist. Skipping.")
            return False
        return True

    def _process_known_images(self, known_dir):
        logger.info(f"Processing known images ({known_dir}) ...")
        known_dir = Path(known_dir)
        if not known_dir.exists():
            return False

        for person_dir in known_dir.glob("*/"):
            if person_dir.is_dir():
                person_name = person_dir.name
                
                for img_path in person_dir.glob("*"):
                     if img_path.is_file() and img_path.suffix.lower() in ['.jpg', '.jpeg', '.png', '.webp']:
                        img = self.load_image(img_path)
                        if img is None: continue

                        faces = self.detector.detect(img)
                        
                        if not faces:
                            logger.warning(f"⚠ No faces detected in: {img_path.name}")
                            continue
                        
                        emb = self.recognizer.get_normalized_embedding(img, faces[0].landmarks)
                        self.db.add_face(img_path, emb, person_name)

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
