import os
import sys
import torch
import warnings
from pathlib import Path
from PIL import Image
import logging
from tqdm import tqdm

warnings.filterwarnings("ignore")

# Add current directory to path to import dataset_tool
sys.path.append(str(Path(__file__).parent))

from dataset_api_service import Tag
from dataset_tagger import DatasetTagger
from transformers import CLIPProcessor, CLIPModel

logger = logging.getLogger(__name__)

TAGGER_TAG_NAME = "tagger.aesthetic.processed"
SCORE_TAG_NAME = "tagger.aesthetic.score"

TAG_INFO_META = {
    TAGGER_TAG_NAME: {
        "description": "Marker tag indicating the item was processed by the aesthetic tagger.",
        "alias": None,
        "internal": True,
        "sortable": False,
        "showInHelp": False,
        "type": "BOOLEAN",
    },
    SCORE_TAG_NAME: {
        "description": "Aesthetic score of the image (0-10).",
        "alias": "aesthetic.score",
        "internal": False,
        "sortable": True,
        "showInHelp": True,
        "type": "DOUBLE",
    },
}

class AestheticPredictor(torch.nn.Module):
    """
    LAION Aesthetic Predictor V2:
    MLP over embeddings CLIP ViT-L/14
    """
    def __init__(self):
        super().__init__()
        self.layers = torch.nn.Sequential(
            torch.nn.Linear(768, 1024),
            torch.nn.Dropout(0.2),
            torch.nn.Linear(1024, 128),
            torch.nn.Dropout(0.2),
            torch.nn.Linear(128, 64),
            torch.nn.Dropout(0.1),
            torch.nn.Linear(64, 16),
            torch.nn.Linear(16, 1),
        )

    def forward(self, x):
        return self.layers(x)

class AestheticTagger(DatasetTagger):
    def __init__(self):
        super().__init__(description="Score images using LAION Aesthetic V2")
        self.device = None
        self.clip_model = None
        self.clip_processor = None
        self.aesthetic_model = None

    def add_custom_args(self, parser):
        parser.add_argument("--force", action="store_true", help="Force reprocessing of all items")

    def get_search_query(self) -> str:
        if self.args.force:
             return "type:I"
        return f"type:I missing_tag:{TAGGER_TAG_NAME}"

    def setup(self):
        if not self._ensure_tag_infos(
            (TAGGER_TAG_NAME, SCORE_TAG_NAME),
            TAG_INFO_META,
        ):
            return False

        self.device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
        logger.info(f"Using device: {self.device}")

        # Loading models
        logger.info("Loading models...")
        self.clip_model = CLIPModel.from_pretrained("openai/clip-vit-large-patch14").to(self.device)
        self.clip_processor = CLIPProcessor.from_pretrained("openai/clip-vit-large-patch14")
        self.clip_model.eval()

        self.aesthetic_model = AestheticPredictor()
        weights_path = "sac+logos+ava1-l14-linearMSE.pth"
        if not os.path.exists(weights_path):
            logger.info("Downloading LAION Aesthetic V2 weights...")
            import urllib.request
            url = "https://github.com/christophschuhmann/improved-aesthetic-predictor/raw/main/sac+logos+ava1-l14-linearMSE.pth"
            urllib.request.urlretrieve(url, weights_path)

        self.aesthetic_model.load_state_dict(torch.load(weights_path, map_location=self.device, weights_only=True))
        self.aesthetic_model = self.aesthetic_model.to(self.device)
        self.aesthetic_model.eval()
        
        logger.info("Models loaded correctly.")
        return True

    def process_item(self, item, img):
        tags = []
        if img is not None:
            try:
                # img is a numpy array (RGB) from dataset_tagger's load_image
                pil_img = Image.fromarray(img)
                inputs = self.clip_processor(images=pil_img, return_tensors="pt")
                pixels = inputs["pixel_values"].to(self.device, non_blocking=True)
                
                with torch.no_grad():
                    image_features_raw = self.clip_model.get_image_features(pixel_values=pixels)
                    image_features = image_features_raw if isinstance(image_features_raw, torch.Tensor) else image_features_raw.pooler_output
                    image_features = image_features / image_features.norm(dim=-1, keepdim=True)
                    score = self.aesthetic_model(image_features).squeeze(-1).cpu().item()

                tags.append(Tag(name=SCORE_TAG_NAME, value=round(float(score), 4)))
                if self.is_debug():
                    tqdm.write(f"✓ Score for {item.filename} ({item.id}): {score:.4f}")
            except Exception as e:
                logger.error(f"Error calculating aesthetic score for item {item.id}: {e}")

        tags.append(Tag(name=TAGGER_TAG_NAME, value=True))
        return tags

if __name__ == "__main__":
    tool = AestheticTagger()
    tool.run()
    logger.info("done.")
