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
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8", newline="\n")


def generate_recipes(module):
    modern = module == "neoforge-26.1.2"
    forge = module.startswith("forge-")
    data = ROOT / module / "src/main/resources/data/justgashloot"
    directory = data / ("recipes" if forge else "recipe")

    def ingredient(item):
        return item if modern else {"item": item}

    def enchanted(name):
        return {("neoforge:ingredient_type" if modern else "type"): "justgashloot:exact_enchantment",
                "enchantment": f"minecraft:{name}"}

    def recipe(name, pattern, key):
        write_json(directory / f"{name}.json", {
            "type": "minecraft:crafting_shaped", "category": "misc", "pattern": pattern,
            "key": {letter: ingredient(value) if isinstance(value, str) else value for letter, value in key.items()},
            "result": {("item" if forge else "id"): f"justgashloot:{name}", "count": 1}})
        write_json(data / ("advancements" if forge else "advancement") / "recipes" / f"{name}.json", {
            "parent": "minecraft:recipes/root", "criteria": {
                "has_material": {"trigger": "minecraft:inventory_changed", "conditions": {
                    "items": [{"items": ["minecraft:iron_nugget"]}]}},
                "has_recipe": {"trigger": "minecraft:recipe_unlocked", "conditions": {"recipe": f"justgashloot:{name}"}}},
            "requirements": [["has_material", "has_recipe"]], "rewards": {"recipes": [f"justgashloot:{name}"]}})

    recipe("loot_tablet", ["nnn", "cac", "nnn"], {"n": "minecraft:iron_nugget", "c": "minecraft:cyan_stained_glass_pane", "a": "minecraft:light_blue_stained_glass_pane"})
    recipe("loot_generator", ["frf", "btb", "frf"], {"f": "minecraft:iron_ingot", "r": "minecraft:redstone", "b": "minecraft:chest", "t": "justgashloot:loot_tablet"})
    recipe("upgrade_base", ["nMn", "MUM", "nMn"], {"n": "minecraft:iron_nugget", "M": "minecraft:polished_deepslate", "U": "minecraft:redstone_block"})
    metals = ["iron_block", "gold_block", "diamond_block", "emerald_block", "crying_obsidian"]
    for category, corner in (("energy", "minecraft:redstone_block"), ("speed", "minecraft:clock"),
                             ("quantity", enchanted("fortune")), ("quality", enchanted("looting"))):
        for tier, metal in enumerate(metals, 1):
            recipe(f"{category}_upgrade_{tier}", ["nMn", "MUM", "nMn"], {
                "n": corner, "M": f"minecraft:{metal}",
                "U": "justgashloot:upgrade_base" if tier == 1 else f"justgashloot:{category}_upgrade_{tier - 1}"})
    recipe("player_upgrade", ["nMn", "MUM", "nMn"], {"n": "minecraft:nether_star", "M": "minecraft:iron_block", "U": "justgashloot:upgrade_base"})


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
    en.pop("tooltip.justgashloot.upgrade_pending", None)
    pt.pop("tooltip.justgashloot.upgrade_pending", None)
    translations = {
        "tooltip.justgashloot.active": ("Active loot table: %s", "Loot table ativa: %s"),
        "tooltip.justgashloot.none": ("None", "Nenhuma"),
        "gui.justgashloot.library": ("Loot Tablet — personal collection", "Loot Tablet — coleção pessoal"),
        "gui.justgashloot.search": ("Search loot table ID", "Buscar ID de loot table"),
        "gui.justgashloot.empty": ("No saved loot tables match this search.", "Nenhuma loot table salva corresponde à busca."),
        "gui.justgashloot.owner": ("Player context: %s", "Contexto de jogador: %s"),
        "message.justgashloot.no_table": ("This target has no real loot table to capture. Fixed Lootr contents cannot be copied.", "Este alvo não possui uma loot table real para capturar. Conteúdos fixos do Lootr não podem ser copiados."),
        "message.justgashloot.no_active": ("Select a saved, available loot table with Shift + right click first.", "Selecione uma loot table salva e disponível com Shift + botão direito primeiro."),
        "message.justgashloot.captured": ("Saved and selected: %s", "Salva e selecionada: %s"),
        "message.justgashloot.configured": ("Loot Generator configured: %s", "Loot Generator configurado: %s"),
        "message.justgashloot.unbound_player": ("Reinstall the Player Upgrade to associate it with its installer.", "Reinstale o Player Upgrade para associá-lo ao jogador que o instalar."),
    }
    for key, (english, portuguese) in translations.items():
        en[key] = english
        pt[key] = portuguese
    write_json(assets / "lang/en_us.json", en)
    write_json(assets / "lang/pt_br.json", pt)
    for module in MODULES:
        generate_recipes(module)
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
