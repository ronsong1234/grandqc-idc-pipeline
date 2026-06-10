# GrandQC × NCI Imaging Data Commons Pipeline

[![Open In Colab](https://colab.research.google.com/assets/colab-badge.svg)](https://colab.research.google.com/github/ronsong1234/grandqc-idc-pipeline/blob/main/notebooks/GrandQC_IDC_TCGA_1MPP.ipynb)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

Automated whole-slide image (WSI) quality control pipeline integrating **GrandQC** and **HistoQC** on cancer slides from the [NCI Imaging Data Commons](https://imaging.datacommons.cancer.gov/). Developed as a capstone project at Georgetown University / Harvard Medical School (Fedorov Lab, Martinos Center for Biomedical Imaging).

---

## Overview

This pipeline:
1. Downloads DICOM slides from IDC for specified TCGA/CMB cohorts
2. Converts DICOM → TIFF (memory-safe, with auto-downsampling for large slides)
3. Runs **GrandQC** artifact segmentation at 7× (MPP 1.5) — the paper's benchmark resolution
4. Runs **HistoQC** blur/tissue detection in parallel
5. Produces a per-slide comparison table (`grandqc_histoqc_comparison.csv`) with dual verdicts
6. Exports cleaned GeoJSON annotations + a ready-to-use QuPath project zip

**Key finding:** GrandQC and HistoQC are complementary tools — GrandQC excels at spatial artifact localization (folds, pen markings, OOF, air bubbles), while HistoQC flags blur-heavy slides. Disagreements (GrandQC PASS / HistoQC FAIL) are expected and informative, not errors.

---

## Repository Structure

```
grandqc-idc-pipeline/
├── notebooks/
│   └── GrandQC_IDC_TCGA_1MPP.ipynb   # Main pipeline notebook (v4)
├── qupath/
│   ├── tcga_coad/
│   │   └── grandqc_load_annotations.groovy
│   └── tcga_brca/
│       └── grandqc_load_annotations.groovy
├── patches/
│   └── README.md                      # GrandQC source patch documentation
├── data/
│   └── examples/
│       ├── tcga_coad_qc_summary.csv
│       ├── tcga_coad_grandqc_histoqc_comparison.csv
│       ├── tcga_brca_qc_summary.csv
│       ├── tcga_brca_grandqc_histoqc_comparison.csv
│       └── colorectal__tcga_coad__*.geojson  (example annotation)
├── docs/
│   └── qupath_setup.md
└── README.md
```

---

## Supported Cohorts

| Collection    | Cancer Type      | Native MPP | Pipeline MPP | Status  |
|---------------|------------------|------------|--------------|---------|
| TCGA-COAD     | Colorectal       | 1.0        | 1.0          | ✅ Done  |
| TCGA-BRCA     | Breast           | 1.0        | 1.0          | ✅ Done  |
| TCGA-PRAD     | Prostate         | varies     | 0.5–1.0      | 🔄 In progress |
| CMB-PCA       | Prostate (CMB)   | varies     | 0.5–1.0      | 🔄 In progress |
| CMB-CRC       | Colorectal (CMB) | varies     | 0.5–1.0      | 🔄 In progress |

---

## Quick Start

### Run in Google Colab (recommended)

Click **Open in Colab** above, or open the notebook directly:

```
notebooks/GrandQC_IDC_TCGA_1MPP.ipynb
```

The notebook is self-contained — it installs all dependencies, clones GrandQC, downloads model weights, applies compatibility patches, and runs the full pipeline.

**Runtime:** Google Colab Pro (T4 GPU, ~51 GB RAM recommended for large slides)

### Notebook cells at a glance

| Cell | Purpose |
|------|---------|
| 1    | Install system + Python dependencies |
| 2    | Clone GrandQC + download 7× model weights (cached after first run) |
| 3    | Patch GrandQC source for IDC TIFF compatibility |
| 4    | **CONFIG** — set cohort, `n_slides`, magnification |
| 5    | Download DICOM from IDC (cached) |
| 6    | Convert DICOM → TIFF (memory-safe, auto-downsample if canvas > 4 GB) |
| 7    | Run GrandQC — tissue detection + artifact segmentation at 7× |
| 8    | Compute QC metrics → `qc_summary_v4.csv` |
| 9a   | Install + patch HistoQC for Python 3.12 |
| 9b   | Run HistoQC on all TIFFs |
| 9c   | GrandQC vs HistoQC comparison table |
| 10   | Clean GeoJSONs + package QuPath zip |

---

## QC Metrics & Verdicts

### GrandQC (artifact segmentation)

Metrics are computed as **% of detected tissue area** (not total slide area):

| Column              | Description                              |
|---------------------|------------------------------------------|
| `pct_tissue_of_slide` | % of slide canvas covered by tissue   |
| `pct_no_artifact`   | % of tissue free of artifacts            |
| `pct_fold`          | % of tissue covered by folds             |
| `pct_dark_spot`     | % of tissue with dark spots              |
| `pct_pen_marking`   | % of tissue with pen markings            |
| `pct_air_bubble`    | % of tissue with air bubbles             |
| `pct_out_of_focus`  | % of tissue that is out of focus         |

**Verdict thresholds:**
- `PASS` — `pct_no_artifact ≥ 80%`
- `BORDERLINE` — `50% ≤ pct_no_artifact < 80%`
- `FAIL` — `pct_no_artifact < 50%`

### HistoQC (blur / tissue integrity)

| Column                | Description                                        |
|-----------------------|----------------------------------------------------|
| `hqc_pct_blurry`      | % of tissue flagged as blurry                      |
| `hqc_tissue_removed`  | True if all tissue was removed by cleanup step     |
| `hqc_verdict`         | `PASS` / `FAIL` / `UNRELIABLE`                     |

- `PASS` — `hqc_pct_blurry ≤ 20%`
- `FAIL` — `hqc_pct_blurry > 20%`
- `UNRELIABLE` — HistoQC's `finalProcessingSpur` removed all tissue (common with biopsy cores)

---

## GrandQC Source Patches

GrandQC was not originally designed for IDC-sourced TIFF files. Four patches are applied in Cell 3 (all idempotent — safe to re-run):

| File | Patch | Reason |
|------|-------|--------|
| `wsi_slide_info.py` | MPP tifffile fallback | OpenSlide cannot read MPP from deflate-compressed TIFFs |
| `wsi_tis_detect.py` | MPP fallback + `MIN_TD=512` + torch.tensor copy | Same MPP issue at line 82; `MIN_TD=512` prevents tissue=100% on small slides; PyTorch 2.x requires `.copy()` |
| `main.py` | `weights_only=False` | PyTorch 2.x deprecation of default pickle loading |
| `wsi_process.py` | Tissue mask convention + threshold | IDC TIFFs use tissue=black=0 (opposite of GrandQC default); threshold changed from `> 0.5` to `> 50` |

See [`patches/README.md`](patches/README.md) for full patch details.

---

## QuPath Annotation Viewer

Each pipeline run produces a QuPath project zip containing:
- Converted TIFFs
- Cleaned GeoJSON annotations (one per slide, in `geojsons/7x/`)
- A Groovy loader script
- A README

### Loading annotations in QuPath

See [`docs/qupath_setup.md`](docs/qupath_setup.md) for step-by-step instructions.

**Artifact color scheme:**

| Class            | Color    |
|------------------|----------|
| No Artifact      | 🟢 Green  |
| Fold             | 🟠 Orange |
| OOF              | 🔴 Red    |
| Air Bubble       | 🔵 Cyan   |
| Pen Marking      | 🟣 Magenta|
| Dark Spot        | ⚫ Gray   |

---

## Technical Notes

- **Magnification:** 7× (MPP 1.5) — matches the GrandQC paper benchmark (Dice 0.808)
- **Large slide handling:** Slides with canvas > 4 GB are auto-downsampled to 1.0 MPP before TIFF conversion
- **Memory:** wsidicom streaming is used during DICOM → TIFF conversion to avoid RAM doubling
- **GeoJSON cleaning:** Shapely `buffer(0)` + `make_valid()` applied to all polygons for QuPath/JTS compatibility

---

## Dependencies

All dependencies are installed automatically by Cell 1–2 of the notebook.

**Core:**
- `idc-index` — IDC DICOM data access
- `openslide-python`, `tifffile` — slide I/O
- `wsidicom` — memory-safe DICOM streaming
- `torch`, `timm`, `segmentation-models-pytorch` — GrandQC inference
- `histoqc` — blur / tissue QC
- `shapely` — GeoJSON geometry validation
- `pandas`, `numpy`, `scikit-image`, `opencv-python-headless`

---

## Citation

If you use this pipeline, please cite:

**GrandQC:**
> Tolkach Y, et al. *Artificial intelligence-based quality control of whole slide images for downstream digital pathology analysis.* Nature Communications, 2024. https://doi.org/10.1038/s41467-024-54769-0

**NCI Imaging Data Commons:**
> Fedorov A, et al. *NCI Imaging Data Commons.* Cancer Research, 2021. https://doi.org/10.1158/0008-5472.CAN-21-0950

**HistoQC:**
> Janowczyk A, et al. *HistoQC: An Open-Source Quality Control Tool for Digital Pathology Slides.* JCO Clinical Cancer Informatics, 2019. https://doi.org/10.1200/CCI.18.00157

---

## License

MIT — see [LICENSE](LICENSE).

This project was developed at Georgetown University (MS Health Informatics and Data Science) in collaboration with Harvard Medical School's Fedorov Lab (Martinos Center for Biomedical Imaging) and MedStar Health Research Institute.
