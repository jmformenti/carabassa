import sys
from pathlib import Path
from tqdm import tqdm

# Add current directory to path to import dataset_tool
sys.path.append(str(Path(__file__).parent))

from dataset_tool import DatasetTool

class CheckDatasetImagesTool(DatasetTool):
    def __init__(self):
        super().__init__(description="Walk through all images in a Carabassa dataset")

    def process_item(self, item, img):
        tqdm.write(f"Processing item {item.id} ..", file=sys.stdout)
        return []


if __name__ == "__main__":
    tool = CheckDatasetImagesTool()
    tool.run()
