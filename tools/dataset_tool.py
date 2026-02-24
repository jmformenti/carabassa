import argparse
from concurrent.futures import Future, ThreadPoolExecutor
import logging
import os
import sys
from pathlib import Path
from typing import List
import cv2
from tqdm import tqdm

# Add current directory to path to import dataset_api_service
sys.path.append(str(Path(__file__).parent))
from dataset_api_service import DatasetApiService, Tag

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s',
    datefmt='%Y-%m-%d %H:%M:%S',
    stream=sys.stdout
)
logger = logging.getLogger(__name__)

class DatasetTool:
    """
    Base class for creating tools that process Carabassa datasets and add tags to images.
    """
    def __init__(self, description: str):
        self.parser = argparse.ArgumentParser(description=description)
        self.parser.add_argument("--dataset", type=str, required=True, help="Name of the Carabassa dataset to process")
        self.parser.add_argument("--api-url", type=str, default=os.environ.get("CARABASSA_BASE_URL", "http://localhost:8080/api/"), help="Carabassa API URL")
        
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

    def run(self):
        """
        Main execution method.
        """
        self.args = self.parser.parse_args()
        logger.info(f"Processing dataset '{self.args.dataset}' with API URL '{self.args.api_url}'")
        self.service = DatasetApiService(self.args.api_url)

        try:
            self.dataset_id = self.service.find_by_name(self.args.dataset)
        except Exception as e:
            logger.error(f"Error finding dataset '{self.args.dataset}': {e}")
            return

        if not self.setup():
            return

        self._process_dataset_items()
        logger.info("Done.")


    def get_search_query(self) -> str:
        """
        Return the search query string to filter items.
        Default is 'type:I' to find only images.
        """
        return "type:I"

    def _process_dataset_items(self):
        logger.info(f"Fetching items info for dataset ID {self.dataset_id}...")
        
        BATCH_SIZE = 100
        search_query = self.get_search_query()

        first_result = self.service.find_items(self.dataset_id, search_string=search_query, page=0, size=BATCH_SIZE)
        
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
                next_page_future = self._get_page(current_page + 1, executor, BATCH_SIZE)

            while True:
                if not items and current_page > 0:
                    break

                for item in items:
                    self._handle_single_item(item, pbar)
                    pbar.update(1)

                if current_page >= total_pages - 1:
                    break

                current_page += 1
                paged_result = next_page_future.result(timeout=30)
                items = paged_result.content

                if current_page < total_pages - 1:
                    next_page_future = self._get_page(current_page + 1, executor, BATCH_SIZE)
                else:
                    next_page_future = None

    def _get_page(self, page, executor, batch_size):
        return executor.submit(
            self.service.find_items,
            self.dataset_id,
            self.get_search_query(),
            page,
            batch_size
        )

    def _handle_single_item(self, item, pbar):
        try:
            # Download item to cache
            img_path = self.service.get_item(self.dataset_id, item.id)
            
            img = self.load_image(img_path)
            if img is None:
                tqdm.write(f"⚠ No image for item {item.id} found at: {img_path}", file=sys.stderr)
                return

            tags = self.process_item(item, img)

            if tags:
                for tag in tags:
                    try:
                        self.service.add_item_tag(self.dataset_id, item.id, tag)
                    except Exception as e:
                        tqdm.write(f"Failed to add tag to item {item.filename} ({item.id}): {e}", file=sys.stderr)
        
        except Exception as e:
            tqdm.write(f"Error processing item {item.id} ({item.filename}): {e}", file=sys.stderr)
