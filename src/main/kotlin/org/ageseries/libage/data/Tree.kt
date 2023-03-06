package org.ageseries.libage.data

open class BinaryTree<T>(var root: Vertex<T>?) {
    constructor(value: T) : this(Vertex(value))
    enum class Direction {
        LEFT, RIGHT;
        val opposite get() = when(this) {
            LEFT -> RIGHT
            RIGHT -> LEFT
        }
    }

    enum class Traversal {
        PREORDER, INORDER, POSTORDER;
        fun<T> visit(root: Vertex<T>?, block: (Vertex<T>) -> Unit) {
            if(root == null) return
            if(this == PREORDER) block(root)
            visit(root.left, block)
            if(this == INORDER) block(root)
            visit(root.right, block)
            if(this == POSTORDER) block(root)
        }
    }

    class Path(val path: List<Direction> = emptyList()) {
        data class Resolution<T>(val vertex: Vertex<T>?, val depth: Int)
        fun<T> resolve(root: Vertex<T>?): Resolution<T> {
            var here = root
            for((depth, dir) in path.withIndex()) {
                if(here == null) return Resolution(null, depth)
                here = here.child(dir)
            }
            return Resolution(here, path.size)
        }

        fun up() = if(path.isEmpty()) { this } else { Path(path.subList(0, path.size - 1)) }
        fun left() = Path(path + listOf(Direction.LEFT))
        fun right() = Path(path + listOf(Direction.RIGHT))

        val isEmpty get() = path.isEmpty()
        fun last() = if(isEmpty) { null } else { path.last() }
    }

    class BFSIter<T>(val root: Vertex<T>?): Iterator<Vertex<T>> {
        var stack = mutableListOf<Pair<Path, Vertex<T>>>().apply {
            root?.let {
                add(Path() to root)
            }
        }

        override fun hasNext(): Boolean = stack.isNotEmpty()

        override fun next(): Vertex<T> {
            val (path, vert) = stack.removeLast()
            if(vert.right != null) stack.add(path.right() to vert.right!!)
            if(vert.left != null) stack.add(path.left() to vert.left!!)
            return vert
        }

    }

    open class Vertex<T>(
        var value: T,
        var left: Vertex<T>? = null,
        var right: Vertex<T>? = null,
        var parent: Vertex<T>? = null,
    ): Iterable<Vertex<T>> {
        fun unlink() {
            parent?.let {
                if(this == it.left) it.left = null
                if(this == it.right) it.right = null
            }
            parent = null
        }

        fun insert(dir: Direction, v: T) = Vertex(v).also { setChild(dir, it) }

        val root: Vertex<T> get() = parent?.root ?: this
        val isLeaf get() = left == null && right == null
        val isFull get() = left != null && right != null

        fun direction(child: Vertex<T>): Direction? = when(child) {
            left -> Direction.LEFT
            right -> Direction.RIGHT
            else -> null
        }
        fun child(dir: Direction) = when(dir) {
            Direction.LEFT -> left
            Direction.RIGHT -> right
        }
        fun setChild(dir: Direction, child: Vertex<T>?) = when(dir) {
            Direction.LEFT -> left = child?.also { it.unlink(); it.parent = this }
            Direction.RIGHT -> right = child?.also { it.unlink(); it.parent = this }
        }

        fun rotate(dir: Direction): Boolean {
            val parDir = parent?.run { direction(this)!! }
            val opp = dir.opposite
            val pivot = child(opp) ?: return false
            val inner = pivot.child(dir)
            parDir?.let { parent!!.setChild(it, pivot) }
            setChild(opp, inner)
            return true
        }

        fun traverse(traversal: Traversal = Traversal.INORDER, block: (Vertex<T>) -> Unit) =
            traversal.visit(this, block)

        override fun iterator(): Iterator<Vertex<T>> = BFSIter(this)
    }
}