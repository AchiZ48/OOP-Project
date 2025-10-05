import java.util.function.Consumer;
import java.awt.Graphics2D;

class DepthRenderable {
    final double depth;
    final Consumer<Graphics2D> drawCommand;

    DepthRenderable(double depth, Consumer<Graphics2D> drawCommand) {
        this.depth = depth;
        this.drawCommand = drawCommand;
    }
}
