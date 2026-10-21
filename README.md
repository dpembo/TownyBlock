# TownyBlock

Stops players creating a town, or expanding one, on chunks you have marked. Aimed at keeping spawn and other admin areas unclaimed.

Requires Paper (or a fork) and Towny. Build with Maven, then put `target/TownyBlock-1.0.0.jar` in the server `plugins` folder.

```bash
mvn package
```

## Commands

Permission: `townyblock.admin` (operators by default). Alias: `/tb`.

| Command | Effect |
| --- | --- |
| `/townyblock radius <radius>` | Preview your chunk and every chunk within that many chunks, a square. Radius 2 is 5 by 5. Red dust marks each block of ground. Nothing is saved yet. |
| `/townyblock confirm` | Save the preview. The dust stops. |
| `/townyblock cancel` | Stop the preview without saving. |
| `/townyblock remove` | Standing inside a saved area, remove that whole area. A single blocked chunk is removed on its own. |
| `/townyblock remove radius <radius>` | Unblock saved areas that sit entirely inside that square, and any single chunks inside it. |
| `/townyblock add` | Block the chunk you are standing in, straight away. |
| `/townyblock add <x> <z>` | Block that chunk in your current world. |
| `/townyblock add <world> <x> <z>` | Block a chunk from the console or another world. |
| `/townyblock list [page]` | Show blocked areas and chunks. |
| `/townyblock reload` | Reload `config.yml` and `chunks.yml`. |

Coordinates are Minecraft chunk coordinates, not block coordinates. Chunk `0, 0` is blocks 0–15 on X and Z.

Blocked chunks are stored in `plugins/TownyBlock/chunks.yml`. A claim is refused when the town block covers any listed chunk. That includes a new town's home block, normal claims, outposts, and takeovers. The denial text is `messages.blocked-claim` in `config.yml`.
