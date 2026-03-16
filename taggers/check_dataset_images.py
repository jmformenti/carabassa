import sys
from pathlib import Path
from tqdm import tqdm

# Add current directory to path to import dataset_tool
sys.path.append(str(Path(__file__).parent))

from dataset_tagger import DatasetTagger

class CheckDatasetImagesTool(DatasetTagger):
    def __init__(self):
        super().__init__(description="Walk through all images in a Carabassa dataset")

    def process_item(self, item, img):
        tqdm.write(f"Reading item {item.id} ..", file=sys.stdout)
        return []


if __name__ == "__main__":
    tool = CheckDatasetImagesTool()
    tool.run()
