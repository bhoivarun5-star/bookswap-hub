/**
 * BookSwap Hub – Theme Toggle
 * Persists dark/light preference to localStorage.
 * Applied ASAP (before paint) to prevent flash.
 */
(function () {
    const saved = localStorage.getItem('bsh-theme') || 'dark';
    document.documentElement.setAttribute('data-theme', saved);
})();

function toggleTheme() {
    const root = document.documentElement;
    const current = root.getAttribute('data-theme') || 'dark';
    const next = current === 'dark' ? 'light' : 'dark';
    root.setAttribute('data-theme', next);
    localStorage.setItem('bsh-theme', next);

    // update all toggle buttons
    document.querySelectorAll('.theme-toggle-btn').forEach(btn => {
        btn.textContent = next === 'dark' ? '☀️' : '🌙';
        btn.title = next === 'dark' ? 'Switch to Light Mode' : 'Switch to Dark Mode';
    });
}

// Set correct icon once DOM is ready
document.addEventListener('DOMContentLoaded', () => {
    const theme = document.documentElement.getAttribute('data-theme') || 'dark';
    document.querySelectorAll('.theme-toggle-btn').forEach(btn => {
        btn.textContent = theme === 'dark' ? '☀️' : '🌙';
        btn.title = theme === 'dark' ? 'Switch to Light Mode' : 'Switch to Dark Mode';
    });

    // ─── Mobile hamburger menu ───────────────────────────────────────
    const hamburger = document.querySelector('.hamburger-btn');
    const navLinks  = document.querySelector('.nav-links');
    const overlay   = document.querySelector('.nav-overlay');
    if (hamburger && navLinks) {
        hamburger.addEventListener('click', () => {
            const isOpen = navLinks.classList.toggle('open');
            hamburger.classList.toggle('open', isOpen);
            if (overlay) overlay.classList.toggle('show', isOpen);
            document.body.style.overflow = isOpen ? 'hidden' : '';
        });
        // Close when clicking overlay
        if (overlay) {
            overlay.addEventListener('click', () => {
                navLinks.classList.remove('open');
                hamburger.classList.remove('open');
                overlay.classList.remove('show');
                document.body.style.overflow = '';
            });
        }
        // Close when a nav link is clicked (except theme toggle)
        navLinks.querySelectorAll('.nav-link').forEach(link => {
            link.addEventListener('click', () => {
                navLinks.classList.remove('open');
                hamburger.classList.remove('open');
                if (overlay) overlay.classList.remove('show');
                document.body.style.overflow = '';
            });
        });
    }
});
