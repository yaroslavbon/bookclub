/**
 * Handles theme switching functionality for the Book Club application
 */
document.addEventListener('DOMContentLoaded', function() {
    // Get elements
    const themeToggle = document.getElementById('themeToggle');
    const lightIcon = document.getElementById('lightIcon');
    const darkIcon = document.getElementById('darkIcon');

    // Use document.documentElement (html tag) for theme attribute
    const htmlElement = document.documentElement;

    // Initialize theme
    initializeTheme();

    // Add toggle event listener
    if (themeToggle) {
        themeToggle.addEventListener('click', toggleTheme);
    }

    /**
     * Initialize theme based on saved preference or system setting
     */
    function initializeTheme() {
        // Check saved preference or use system preference
        const savedTheme = localStorage.getItem('bookclub-theme');
        if (savedTheme) {
            // User has explicitly set a preference
            savedTheme === 'dark' ? applyDarkTheme() : applyLightTheme();
        } else {
            // Check system preference
            if (window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches) {
                applyDarkTheme();
            } else {
                applyLightTheme();
            }

            // Listen for system theme changes
            window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', e => {
                // Only auto-switch if user hasn't manually set a preference
                if (!localStorage.getItem('bookclub-theme')) {
                    e.matches ? applyDarkTheme() : applyLightTheme();
                }
            });
        }
    }

    /**
     * Toggle between light and dark themes
     */
    function toggleTheme() {
        if (htmlElement.getAttribute('data-bs-theme') === 'dark') {
            applyLightTheme();
            localStorage.setItem('bookclub-theme', 'light');
        } else {
            applyDarkTheme();
            localStorage.setItem('bookclub-theme', 'dark');
        }
    }

    /**
     * Apply dark theme
     */
    function applyDarkTheme() {
        htmlElement.setAttribute('data-bs-theme', 'dark');
        updateIcons(true);
    }

    /**
     * Apply light theme
     */
    function applyLightTheme() {
        htmlElement.setAttribute('data-bs-theme', 'light');
        updateIcons(false);
    }

    /**
     * Update icon visibility based on current theme
     */
    function updateIcons(isDark) {
        if (!lightIcon || !darkIcon) return;

        lightIcon.style.display = isDark ? 'none' : 'block';
        darkIcon.style.display = isDark ? 'block' : 'none';
    }
});