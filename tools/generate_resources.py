"""Copy original art and generate versioned Minecraft resource definitions."""
from pathlib import Path
import json
import os
import shutil
import subprocess

ROOT = Path(__file__).resolve().parents[1]
COMMON = ROOT / "common/src/main/resources"
MODULES = ("forge-1.20.1", "neoforge-1.21.1", "neoforge-26.1.2")


def write_json(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def main():
    assets = COMMON / "assets/justgashloot"
    textures = {
        "block/loot_generator_side": "Loot_Generator_side.png",
        "block/loot_generator_top": "lootGeneratorTop.png",
        "item/upgrade_base": "Upgrade_Base.png",
        "item/loot_tablet": "loot_tablet.png",
        "item/player_upgrade": "Player.png",
    }
    for category, source in (("energy", "Energy"), ("speed", "Speed"),
                             ("quantity", "Quantity"), ("quality", "Quallity")):
        for tier in range(1, 6):
            textures[f"item/{category}_upgrade_{tier}"] = f"{source}_{tier}.png"
    for name, original in textures.items():
        target = assets / f"textures/{name}.png"
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(ROOT / "assets" / original, target)
    # Minecraft shares its block/item atlas. Irregular dimensions disable mipmaps globally.
    java_home = os.environ.get("JAVA_HOME")
    java = str(Path(java_home) / "bin" / ("java.exe" if os.name == "nt" else "java")) if java_home else "java"
    subprocess.run([java, str(ROOT / "tools/PrepareTextures.java"), str(assets / "textures")], check=True)

    write_json(assets / "blockstates/loot_generator.json", {
        "variants": {"": {"model": "justgashloot:block/loot_generator"}}})
    write_json(assets / "models/block/loot_generator.json", {
        "parent": "minecraft:block/cube_bottom_top",
        "textures": {"side": "justgashloot:block/loot_generator_side",
                     "top": "justgashloot:block/loot_generator_top",
                     "bottom": "minecraft:block/smithing_table_top"}})
    write_json(assets / "models/item/loot_generator.json", {
        "parent": "justgashloot:block/loot_generator"})
    items = [name.split("/")[1] for name in textures if name.startswith("item/")]
    for name in items:
        write_json(assets / f"models/item/{name}.json", {
            "parent": "minecraft:item/generated", "textures": {"layer0": f"justgashloot:item/{name}"}})
    for name in ["loot_generator", *items]:
        write_json(ROOT / "neoforge-26.1.2/src/main/resources/assets/justgashloot/items" / f"{name}.json", {
            "model": {"type": "minecraft:model", "model": f"justgashloot:item/{name}"}})

    en = {"itemGroup.justgashloot": "Just Gash Loot",
          "block.justgashloot.loot_generator": "Loot Generator",
          "gui.justgashloot.upgrades": "Upgrades",
          "gui.justgashloot.count": "Stored: %s",
          "gui.justgashloot.more": "+%s item types (hold Shift for details)",
          "config.jade.plugin_justgashloot.bulk_items": "Loot Generator inventory",
          "item.justgashloot.upgrade_base": "Upgrade Base",
          "item.justgashloot.loot_tablet": "Loot Tablet",
          "item.justgashloot.player_upgrade": "Player Upgrade",
          "tooltip.justgashloot.upgrade_pending": "Initial structure: upgrade effects are not yet enabled.",
          "message.justgashloot.large_drop": "This Loot Generator will drop more than 300 stacks. Sneak (Shift) to confirm. Lava and FE will be lost."}
    pt = {"itemGroup.justgashloot": "Just Gash Loot",
          "block.justgashloot.loot_generator": "Gerador de Loot",
          "gui.justgashloot.upgrades": "Upgrades",
          "gui.justgashloot.count": "Armazenado: %s",
          "gui.justgashloot.more": "+%s tipos de itens (segure Shift para detalhes)",
          "config.jade.plugin_justgashloot.bulk_items": "Inventário do Gerador de Loot",
          "item.justgashloot.upgrade_base": "Base de Upgrade",
          "item.justgashloot.loot_tablet": "Tablet de Loot",
          "item.justgashloot.player_upgrade": "Upgrade de Jogador",
          "tooltip.justgashloot.upgrade_pending": "Estrutura inicial: os efeitos dos upgrades ainda não estão ativos.",
          "message.justgashloot.large_drop": "Este Loot Generator derrubará mais de 300 pilhas. Agache (Shift) para confirmar. Lava e FE serão perdidos."}
    for category, english, portuguese in (("energy", "Energy", "Energia"), ("speed", "Speed", "Velocidade"),
                                          ("quantity", "Quantity", "Quantidade"), ("quality", "Quality", "Qualidade")):
        for tier in range(1, 6):
            key = f"item.justgashloot.{category}_upgrade_{tier}"
            en[key] = f"{english} Upgrade {tier}"
            pt[key] = f"Upgrade de {portuguese} {tier}"
    write_json(assets / "lang/en_us.json", en)
    write_json(assets / "lang/pt_br.json", pt)
    for module in MODULES:
        resource = ROOT / module / "src/main/resources"
        modern = module == "neoforge-26.1.2"
        write_json(resource / "data/justgashloot" / ("loot_tables" if module.startswith("forge-") else "loot_table") / "blocks/loot_generator.json", {
            "type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": "justgashloot:loot_generator"}]}]})
        if not modern:
            # The block can be collected with any pickaxe and also by hand in this initial version.
            write_json(resource / "data/minecraft/tags" / ("blocks" if module.startswith("forge-") else "block") / "mineable/pickaxe.json", {
                "replace": False, "values": ["justgashloot:loot_generator"]})


if __name__ == "__main__":
    main()
