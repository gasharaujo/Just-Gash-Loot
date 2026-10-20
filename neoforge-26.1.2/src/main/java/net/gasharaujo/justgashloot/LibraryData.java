package net.gasharaujo.justgashloot;
public final class LibraryData extends net.minecraft.world.level.saveddata.SavedData {
    public LibraryBook book = new LibraryBook();
    public LibraryData() {}
    private static final net.minecraft.world.level.saveddata.SavedDataType<LibraryData> TYPE = new net.minecraft.world.level.saveddata.SavedDataType<>(
            net.minecraft.resources.Identifier.fromNamespaceAndPath("justgashloot", "tablet_library"), LibraryData::new,
            com.mojang.serialization.Codec.withAlternative(
                    LibraryBook.CODEC.fieldOf("Players").codec().xmap(LibraryData::fromBook, data -> data.book),
                    com.mojang.serialization.Codec.STRING.fieldOf("Payload").codec().xmap(LibraryData::decode, data -> data.book.encode())));
    private static LibraryData fromBook(LibraryBook book) { LibraryData data = new LibraryData(); data.book = book; return data; }
    private static LibraryData decode(String value) { LibraryData data = new LibraryData(); data.book = LibraryBook.decode(value); return data; }
    public static LibraryData get(net.minecraft.server.MinecraftServer server) { return server.overworld().getDataStorage().computeIfAbsent(TYPE); }
}
