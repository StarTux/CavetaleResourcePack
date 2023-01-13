# Cavetale Resource Pack

Create a reproducible resource pack based on data provided by Mytems
and Core.  Some special propreties may be hardcoded.

## Bow Pulling

Bow pulling animations are generated if the files are present:
- `bow_name.png`
- `bow_name_pulling_0.png`
- `bow_name_pulling_1.png`
- `bow_name_pulling_2.png`

## Animations

Font glyphs of animated textures chat will generally use the topmost
frame.  Exceptions will be hardcoded here.

The distinguishing legacy file name `item_name_animated.png` is no
longer required.

Where the mcmeta file is absent in the source resourcepack for an
animated Mytems texture, it will be synthesized provided the Mytems
enum provides an Animation instance.

## Build Default Font

TODO write me a procedure so I don't forget each time a new version is
released.