import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;

/** Build-time format conversion only. Original artwork in assets/ is never modified. */
public final class PrepareTextures {
    public static void main(String[] args) throws Exception {
        int converted = 0;
        try (var paths = Files.walk(Path.of(args[0]))) {
            for (Path path : paths.filter(file -> file.toString().endsWith(".png")).toList()) {
                BufferedImage input = ImageIO.read(path.toFile());
                int width = input.getWidth(), height = input.getHeight();
                if (width == height && (width & (width - 1)) == 0) continue;
                int size = 16;
                while (size < Math.max(width, height)) size *= 2;
                double scale = (double) size / Math.max(width, height);
                int targetWidth = Math.max(1, (int) Math.round(width * scale));
                int targetHeight = Math.max(1, (int) Math.round(height * scale));
                BufferedImage output = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
                Graphics2D graphics = output.createGraphics();
                graphics.setComposite(AlphaComposite.Src);
                graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                graphics.drawImage(input, (size - targetWidth) / 2, (size - targetHeight) / 2, targetWidth, targetHeight, null);
                graphics.dispose();
                if (!ImageIO.write(output, "PNG", path.toFile())) throw new IllegalStateException("PNG writer unavailable");
                converted++;
            }
        }
        System.out.println("Prepared " + converted + " game textures; original art preserved.");
    }
}
