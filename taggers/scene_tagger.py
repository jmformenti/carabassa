import sys
from pathlib import Path
import torch
import logging
from transformers import CLIPProcessor, CLIPModel
from PIL import Image
from typing import List
from tqdm import tqdm
from transformers import logging as hf_logging

# Add current directory to path to import dataset_tool
sys.path.append(str(Path(__file__).parent))
from dataset_tagger import DatasetTagger
from dataset_api_service import Tag

logger = logging.getLogger(__name__)

# Suppress verbose HTTP request logs from underlying libraries
logging.getLogger("httpx").setLevel(logging.WARNING)
logging.getLogger("urllib3").setLevel(logging.WARNING)
logging.getLogger("huggingface_hub").setLevel(logging.WARNING)
logging.getLogger("filelock").setLevel(logging.WARNING)
hf_logging.set_verbosity_error()

TAG_NAME = "tagger.scene"
TAGGER_TAG_NAME = "tagger.scene.processed"

MIN_ABSOLUTE_PROB = 0.60
RELATIVE_PROB_THRESHOLD = 0.80

SCENES_MAPPING = {
    "in the forest": "forest",
    "on the beach": "beach",
    "in the sea": "sea",
    "in the mountain": "mountain",
    "in the city": "city",
    "in nature": "nature",
    "in the park": "park",
    "at home": "home",
    "in an office": "office",
    "in a car": "car",
    "in a boat": "boat",
    "in a plane": "plane",
    "in a train": "train",
    "in a bus": "bus",
    "in a restaurant": "restaurant",
    "other": "other"
}
SCENES = list(SCENES_MAPPING.keys())

TAG_INFO_META = {
    TAG_NAME: {
        "description": "Detected scene from image.",
        "alias": "scene",
        "internal": False,
        "sortable": True,
        "type": "STRING",
    },
    TAGGER_TAG_NAME: {
        "description": "Marker tag indicating the item was processed by the scene tagger.",
        "alias": None,
        "internal": True,
        "sortable": False,
        "type": "BOOLEAN",
    },
}

class SceneTagger(DatasetTagger):
    def __init__(self):
        super().__init__(description="Scene Detection Tagger")
        self.model = None
        self.processor = None
        self.device = None

    def add_custom_args(self, parser):
        parser.add_argument("--device", default=None, help="Device: cuda, mps, cpu (default: auto)")
        parser.add_argument("--force", action="store_true", help="Force reprocessing of all items")
        parser.add_argument("--test-image", type=str, help="Path to an image to test scene detection (will not modify dataset)")

    def get_search_query(self) -> str:
        if self.args.force:
             return "type:I"
        return f"type:I missing_tag:{TAGGER_TAG_NAME}"

    def _init_model(self):
        if self.model is not None:
            return

        if self.args.device:
            self.device = self.args.device
        elif torch.cuda.is_available():
            self.device = "cuda"
        elif torch.backends.mps.is_available():
            self.device = "mps"
        else:
            self.device = "cpu"

        logger.info(f"Using device: {self.device}")
        
        self.model = CLIPModel.from_pretrained("openai/clip-vit-base-patch32").to(self.device)
        self.processor = CLIPProcessor.from_pretrained("openai/clip-vit-base-patch32")

    def _detect_scenes(self, pil_image):
        inputs = self.processor(text=SCENES, images=pil_image, return_tensors="pt", padding=True)
        inputs = {k: v.to(self.device) for k, v in inputs.items()}
        
        with torch.no_grad():
            outputs = self.model(**inputs)
        
        probs = outputs.logits_per_image.softmax(dim=1)
        probs_list = probs[0].tolist()
        scenes_with_probs = list(zip(SCENES, probs_list))
        scenes_with_probs.sort(key=lambda x: x[1], reverse=True)
        
        max_prob = scenes_with_probs[0][1]
        most_probable_scenes = [scene for scene, prob in scenes_with_probs if prob >= max_prob * RELATIVE_PROB_THRESHOLD and prob >= MIN_ABSOLUTE_PROB]
        
        if len(most_probable_scenes) > 2:
            most_probable_scenes = []
            
        return scenes_with_probs, most_probable_scenes

    def setup(self):
        if not self._ensure_tag_infos(
            (TAG_NAME, TAGGER_TAG_NAME),
            TAG_INFO_META,
        ):
            return False

        self._init_model()
        return True

    def handle_test_mode(self):
        if not hasattr(self.args, "test_image") or not self.args.test_image:
            return False

        self._init_model()

        img = self.load_image(self.args.test_image)
        if img is None:
            logger.error(f"Cannot load image {self.args.test_image}")
            return True

        pil_image = Image.fromarray(img)
        scenes_with_probs, most_probable_scenes = self._detect_scenes(pil_image)
        
        for scene, prob in scenes_with_probs:
            print(f"{scene}: {prob:.1%}")

        print("\nMost probable scene(s):")
        if not most_probable_scenes:
            print(f"⭐ other (fallback, distribution unclear)")
        else:
            for scene in most_probable_scenes:
                if scene == "other": continue
                prob_value = next(p for s, p in scenes_with_probs if s == scene)
                base_scene = SCENES_MAPPING.get(scene, scene)
                print(f"⭐ {base_scene} ({prob_value:.1%})")

        return True

    def process_item(self, item, img) -> List[Tag]:
        if self.args.force:
            full_item = self.service.find_item(self.dataset_id, item.id)
            if full_item.tags:
                for t in full_item.tags:
                    if t.get("name") in (TAG_NAME, TAGGER_TAG_NAME):
                        try:
                            self.service.delete_item_tag(self.dataset_id, item.id, t.get("id"))
                        except Exception as e:
                            tqdm.write(f"Failed to delete old tag {t.get('name')} for item {item.id}: {e}")
        
        tags = []
        if img is not None:
            try:
                # Convert img back to PIL Image as required by transformers pipeline/processor
                pil_image = Image.fromarray(img)
                
                scenes_with_probs, most_probable_scenes = self._detect_scenes(pil_image)
                
                most_probable_scenes_str = []
                if most_probable_scenes:
                    for scene in most_probable_scenes:
                        if scene == "other":
                            continue
                            
                        prob_value = next(p for s, p in scenes_with_probs if s == scene)
                        base_scene = SCENES_MAPPING.get(scene, scene)
                        tag = Tag(name=TAG_NAME, value=base_scene)
                        tags.append(tag)
                        most_probable_scenes_str.append(f"{base_scene} ({prob_value:.1%})")
                        
                    if most_probable_scenes_str:
                        if self.is_debug():
                            tqdm.write(f"✓ Detected scenes for {item.filename}: {', '.join(most_probable_scenes_str)}")
                    elif self.is_debug():
                        tqdm.write(f"ℹ No clear scene detected for {item.filename}")
                elif self.is_debug():
                    tqdm.write(f"ℹ No clear scene detected for {item.filename}")
                    
            except Exception as e:
                tqdm.write(f"Error processing scene detection for item {item.id}: {e}")
                
        # Add tag to mark item as processed
        tags.append(Tag(name=TAGGER_TAG_NAME, value=True))
        
        return tags

if __name__ == "__main__":
    tool = SceneTagger()
    tool.run()
    logger.info("Done.")
