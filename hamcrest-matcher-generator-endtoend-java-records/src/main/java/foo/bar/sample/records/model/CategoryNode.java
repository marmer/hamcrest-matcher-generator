/*
 * SPDX-FileCopyrightText: 2019-2026 Mariano Mertinat and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package foo.bar.sample.records.model;

import java.util.ArrayList;
import java.util.List;

/** A bean of a bidirectional model: the children point back to their parent. */
public class CategoryNode {
    private final String name;
    private CategoryNode parent;
    private final List<CategoryNode> children = new ArrayList<>();

    public CategoryNode(final String name) {
        this.name = name;
    }

    /** Adds the child and points it back to this node. */
    public CategoryNode withChild(final CategoryNode child) {
        child.parent = this;
        children.add(child);
        return this;
    }

    public String getName() {
        return name;
    }

    public CategoryNode getParent() {
        return parent;
    }

    public List<CategoryNode> getChildren() {
        return children;
    }
}
