// Reproduces modpack filters that remove recipes using an unrelated blocked item.
// Before the fix, both enchanted ingredients advertised every registered item.
ServerEvents.recipes(event => {
    event.remove({ input: 'minecraft:apple' });
});
