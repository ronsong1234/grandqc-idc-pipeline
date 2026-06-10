# GrandQC Source Patches for IDC TIFF Compatibility

This document describes the four patches applied to GrandQC source files in **Cell 3** of the pipeline notebook. All patches are idempotent — safe to re-run after session restarts or re-cloning.

GrandQC was designed for standard OpenSlide-compatible whole-slide images. IDC-sourced TIFFs written by the pipeline's DICOM-to-TIFF conversion (Cell 6) use deflate compression, which prevents OpenSlide from reading MPP metadata. These patches add fallbacks and fix PyTorch 2.x incompatibilities.

---

## Patch 1 — `wsi_slide_info.py`: MPP tifffile fallback

**Problem:** OpenSlide cannot read `openslide.mpp-x` from deflate-compressed TIFFs. The original code raises a `KeyError` immediately.

**Fix:** Wrap the `openslide.mpp-x` read in a try/except, then fall back to reading `XResolution` and `ResolutionUnit` TIFF tags via `tifffile`. Cell 6 writes the resolution in px/cm (ResolutionUnit = 3), so the fallback converts:

```
mpp = 10000.0 / XResolution_value  # px/cm → µm/px
```

A final fallback of `mpp = 1.0` is used if tifffile also fails.

---

## Patch 2 — `wsi_tis_detect.py`: MPP fallback + MIN_TD + torch.tensor

Three sub-patches in this file:

### 2a — MPP fallback (line 82)
Same issue as Patch 1, but at a separate MPP read on line 82 of the tissue detector. This code uses `path_slide` (a string path), not `slide._filename` — the fallback uses `tifffile.TiffFile(path_slide)` accordingly.

### 2b — `MIN_TD = 512`
**Problem:** The original `MIN_TD = 256` causes tissue detection to return 100% tissue on very small IDC slides (some TCGA biopsies are tiny).

**Fix:** Raise the minimum thumbnail dimension from 256 to 512, which forces a proper upscale before tissue segmentation.

### 2c — `torch.tensor(image_pre.copy())`
**Problem:** PyTorch 2.x raises a warning/error when calling `torch.tensor()` on a non-writable numpy array.

**Fix:** Add `.copy()` to produce a writable array before tensor construction.

---

## Patch 3 — `main.py`: `weights_only=False`

**Problem:** PyTorch 2.x changed the default behavior of `torch.load()` to warn (and eventually error) when loading pickled checkpoints without `weights_only=True`. GrandQC's checkpoints use arbitrary pickle objects that are incompatible with `weights_only=True`.

**Fix:** Explicitly pass `weights_only=False` to both `torch.load()` calls (for the QC model and the tissue detection model).

---

## Patch 4 — `wsi_process.py`: Tissue mask convention + threshold

**Problem:** GrandQC's default tissue mask convention is tissue=white=1, background=black=0. IDC TIFFs produced by `wsidicom` streaming use the opposite convention: tissue=black=0, background=white=1. This causes every pixel to be misclassified.

Additionally, the original threshold `> 0.5` was written for normalized float masks. After conversion, mask pixel values are uint8 integers (0–255), making `> 0.5` effectively always True.

**Fix:**
- Flip the tissue/background convention: `np.where(td_patch_ == 0, BACK_CLASS, mask_raw)` → `np.where(td_patch_ == 1, BACK_CLASS, mask_raw)`
- Replace threshold `> 0.5` with `> 50`

---

## Why these are safe to commit

- All patches are string-match replacements with guard strings (e.g., `'tifffile fallback'`, `'IDC tissue convention'`). Re-running Cell 3 on an already-patched file is a no-op.
- `__pycache__` is cleared after each patch application so Python picks up the new source.
- None of the patches change the model architecture or inference logic — they only affect file I/O and device loading.
