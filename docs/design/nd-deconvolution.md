# N-dimensional input to the deconvolution GUI

**Status:** proposed. Phase 1 in progress.

## The problem

The GUI assumes its input is one 3D (XYZ) volume. A multichannel image breaks
cell mode: `ImageJFunctions.convertFloat` turns an XYCZ hyperstack into a 4D
`Img`, the code reads dimension 2 (channels) as Z, and `Lazy.generate` fails with
`Index 3 out of bounds for length 3` for its 3-element cell size. The test case
was the EPFL LLS7 HeLa dataset (Zenodo 14505724): 1024 x 450 x 2 channels x 2001
z, with one measured PSF and no PSF for the second channel.

## The model

Deconvolution is a spatial operation, valid on XY or XYZ only. An image is a set
of independent spatial volumes, and every other axis is a batch axis. Batch axes
come in two kinds:

- **Axes that select the PSF:** channel, because the PSF depends on wavelength.
- **Plain batch axes:** time, stage position. These use the same PSF and only
  add volumes.

A run therefore lists the volumes, settles a PSF per channel, and loops,
deconvolving each volume in cell or whole mode as now.

## A. Batch: which volumes, and saying so

- Show the layout as soon as an input is chosen, e.g.
  `1024 x 450 x 2001 (XYZ), 2 channels, 1 timepoint -> 2 volumes`.
- Channel selection: a checkbox per channel, all on by default.
- Time selection: current / range / all, defaulting to current, so a long
  series is never run by accident.
- Stage positions: Bio-Formats usually opens each series as its own image, so
  positions start as "one image at a time" and get a batch over open images or
  files later.
- Output: one hyperstack with the selected C/T layout and the input's
  calibration. Warn before running if the float output will not fit the heap.
- Execution: per (t, c), hyperslice a view, convert only that volume to float,
  deconvolve. Progress reads "channel 1/2, cell 3/16". Check for cancel between
  volumes.

## B. Matching a PSF to each channel

Read the channel emission and excitation wavelengths from Bio-Formats metadata
when the input is chosen. Show them in an editable channel table; with no
metadata the user types them. Each row shows the channel, its wavelengths, the
PSF it will get and any warning. That table is the PSF plan, visible before Run.

| PSF source | Rule |
|---|---|
| Measured, multichannel PSF with the same channel count | Match by position. |
| Measured, several PSFs with known wavelengths (metadata, or a name like `488`) | Nearest wavelength within a tolerance, as LLSpy does. Flag channels left unmatched. |
| Measured, one PSF, wavelength unknown | Use it for every channel, with the warning "One PSF for N channels; may not be optimal". |
| Measured, one PSF, wavelength known | Same by default. An opt-in option scales it by lambda / lambda0 for the other channels (an approximation). |
| Gibson-Lanni | Wavelength becomes per channel, filled from emission metadata; the other parameters are shared. One PSF per channel. |
| Gaussian | Shared sigma at first. Per-channel scaling later. |
| From beads | A multichannel bead image gives one extracted PSF per channel, matched by position. |

Effects on the PSF tabs: fill the XY and Z spacing from the image calibration,
and warn when the PSF spacing differs from the image's.

Prior art (none of it a reusable Fiji utility): LLSpy matches OTFs by laser
wavelength parsed from file names, napari-lattice takes PSFs in channel order,
Huygens and ZEN match from metadata but are closed source.

## C. Code structure

Move the work out of `RichardsonLucyModelController.runDeconvolution` into small
parts, all testable without a GPU except the last:

- `VolumeBatch`: the (c, t) volumes and spatial axes of the input.
- `ChannelInfo`: wavelengths from metadata, with manual overrides.
- `PsfPlan` / `PsfMatcher`: the rules above, giving channel -> PSF with its
  provenance and any warning. Could later become a general per-channel model
  matcher (Cellpose models, etc.) in shared axis code.
- `VolumeDeconvolver`: one 3D volume, cell or whole mode (today's code).
- The GUI only displays layout, channel table and selections.

Use imagej-common's `ImgPlus` axis types and ImgLib2's `Views.hyperSlice`
directly. Do not depend on fiji-skopkit for this: its Java axis classes are the
front end to a planner that runs in Python, and it would add Appose, menus and a
newer pom-scijava. If a Java slicer/planner grows that both projects want, split
it into a small shared artifact then. Borrow fiji-skopkit's axis spec syntax
(`"x y z c! t"`) if the GUI ever needs an axis field.

## Phases

1. **Feedback only.** When an input image is selected (or is the default at
   startup), print its dimensionality to the GUI's status text: size, channels,
   slices, frames, bit depth, calibration. No behavior change. Lets a user
   confirm the image is what they think before anything else is built.
2. **Volume loop.** Channel and time selection, one PSF for all channels with
   the warning, hyperstack output. Makes multichannel input work.
3. **Channel table.** Metadata wavelengths, per-channel Gibson-Lanni, spacing
   from calibration.
4. **Several measured PSFs.** Matching by position or wavelength, optional
   scaling of a single PSF.
5. **Positions and batch.** Several series or images, and a headless command
   for scripts.

## Open questions

- Default time selection: current timepoint or all?
- Output as one hyperstack, or one image per channel (simpler for very large
  data)?
- Support 2D (XY-only) input? Not yet checked whether the RL code handles 2D.
- Lattice data is skewed, so a theoretical PSF would also need skewing. Out of
  scope for now; a measured PSF in raw space works as is.
