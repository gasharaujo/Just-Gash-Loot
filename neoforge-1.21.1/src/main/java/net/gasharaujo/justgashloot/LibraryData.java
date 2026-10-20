package net.gasharaujo.justgashloot;
public final class LibraryData extends net.minecraft.world.level.saveddata.SavedData {
    public LibraryBook book = new LibraryBook();
    public LibraryData() {}
    private static LibraryData load(net.minecraft.nbt.CompoundTag tag) {
        LibraryData data = new LibraryData();
        data.book = tag.contains("Players") ? LibraryBook.CODEC.parse(net.minecraft.nbt.NbtOps.INSTANCE, tag.get("Players")).result().orElseThrow()
                : LibraryBook.decode(tag.getString("Payload"));
        return data;
    }
    @Override public net.minecraft.nbt.CompoundTag save(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider provider) {
        tag.remove("Payload"); tag.put("Players", LibraryBook.CODEC.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, book).result().orElseThrow()); return tag;
    }
    public static LibraryData get(net.minecraft.server.MinecraftServer server) { return server.overworld().getDataStorage().computeIfAbsent(new Factory<>(LibraryData::new, (tag, provider) -> load(tag), null), "justgashloot_tablet_library"); }
}
