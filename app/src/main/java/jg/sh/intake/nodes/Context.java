package jg.sh.intake.nodes;

public abstract class Context {

    private final Node node;
    private final Context parent;

    public Context(Node node) {
        this(node, null);
    }

    public Context(Node node, Context parent) {
        this.node = node;
        this.parent = parent;
    }

    public Node node() {
        return node;
    }

    public Context parent() {
        return parent;
    }

    public boolean hasParent() {
        return parent != null;
    }
}
