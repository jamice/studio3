package com.aptana.editor.common.theme;

import java.util.Set;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.text.rules.IToken;

public interface IThemeManager
{

	/**
	 * Preference key used to store the timestamp of last theme change. Used to force a redraw of editors when theme
	 * changes (even if it remains same theme, but has been edited).
	 */
	public static final String THEME_CHANGED = "THEME_CHANGED"; //$NON-NLS-1$

	// FIXME Rather than having pref listeners register by knowing the node and everything, have them register through
	// this interface like Eclipse's IThemeManager

	// TODO Make arg the string id, rather than the theme object
	public void setActiveTheme(Theme theme);

	public Theme getActiveTheme();

	public void addTheme(Theme theme);

	public void removeTheme(Theme theme);

	public boolean isBuiltinTheme(String themeName);

	public Set<String> getThemeNames();

	public Theme getTheme(String name);

	public IToken getToken(String name);

	/**
	 * Used to validate that theme name is ok to use.
	 * 
	 * @param name
	 * @return
	 */
	public IStatus validateThemeName(String name);

}