# Cavetale Resource Pack

Create a reproducible resource pack based on data provided by Mytems
and Core.  Some special propreties may be hardcoded.

## Pocket Mobs

Pocket Mobs and their colors are hardcoded in the Mytems class
`PocketMobType`.  The PocketMob plugin has the utility to generate
said enum with color codes.

## Bow Pulling

Bow pulling animations are generated if the files are present:
- `<name>.png`
- `<name>_pulling_N.png`
Each named after the number of ticks.

## Shield blocking

Shield blocking is automatically generated.

## Compass directions are generated based on the existing files:
- `<name>.png`
- `<name>_NNN.png`
Where NNN is a 3-digit number of the current compass angle, from 0 to
360.

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