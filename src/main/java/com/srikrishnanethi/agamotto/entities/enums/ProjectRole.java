package com.srikrishnanethi.agamotto.entities.enums;

/**
 * Collaboration role on a shared project.
 */
public enum ProjectRole {
	OWNER,
	EDITOR,
	VIEWER;

	public boolean canView() {
		return true;
	}

	public boolean canEdit() {
		return this == OWNER || this == EDITOR;
	}

	public boolean canManageMembers() {
		return this == OWNER;
	}
}
