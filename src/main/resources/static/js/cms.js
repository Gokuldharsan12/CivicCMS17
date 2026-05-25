/**
 * CivicCMS — Content Loader  (cms.js)
 * ─────────────────────────────────────────────────────────────────
 * Include this script on any page where you want live CMS content:
 *
 *   <script src="/js/cms.js"></script>
 *
 * Then mark any element you want hydrated with:
 *
 *   data-cms-page="index"  data-cms-key="hero.headline"
 *
 * For images, the src attribute is updated:
 *
 *   <img data-cms-page="global" data-cms-key="logo" src="/images/logo.png">
 *
 * If no CMS value exists for a key the original DOM content is kept as-is.
 * ─────────────────────────────────────────────────────────────────
 */
(function () {
  'use strict';

  /** Cache: page → { key → entry } */
  const _cache = {};

  /**
   * Fetch all content for a given page (deduplicated, cached for the session).
   * @param {string} page
   * @returns {Promise<Object>} key→{value,type,label,id}
   */
  async function fetchPage(page) {
    if (_cache[page]) return _cache[page];
    try {
      const res = await fetch('/api/content?page=' + encodeURIComponent(page));
      if (!res.ok) return {};
      const data = await res.json();
      _cache[page] = data;
      return data;
    } catch {
      return {};
    }
  }

  /**
   * Hydrate a single element using its data-cms-page / data-cms-key attributes.
   * @param {HTMLElement} el
   * @param {Object} pageData
   */
  function hydrateElement(el, pageData) {
    const key   = el.dataset.cmsKey;
    const entry = pageData[key];
    if (!entry || !entry.value) return;

    if (entry.type === 'IMAGE') {
      // For <img> update src; for other elements update background-image or innerHTML
      if (el.tagName === 'IMG') {
        el.src = entry.value;
      } else {
        el.style.backgroundImage = 'url(' + entry.value + ')';
      }
    } else {
      // TEXT — if the value contains HTML tags use innerHTML, otherwise textContent
      if (/<[a-z][\s\S]*>/i.test(entry.value)) {
        el.innerHTML = entry.value;
      } else {
        el.textContent = entry.value;
      }
    }
  }

  /**
   * Main entry-point: scan the page for [data-cms-page] elements and hydrate them.
   * Automatically groups elements by page to minimise HTTP requests.
   */
  async function hydrate() {
    const elements = Array.from(document.querySelectorAll('[data-cms-page][data-cms-key]'));
    if (!elements.length) return;

    // Group by page
    const byPage = {};
    for (const el of elements) {
      const p = el.dataset.cmsPage;
      if (!byPage[p]) byPage[p] = [];
      byPage[p].push(el);
    }

    // Fetch + apply per page (all in parallel)
    await Promise.all(
      Object.entries(byPage).map(async ([page, els]) => {
        const data = await fetchPage(page);
        for (const el of els) hydrateElement(el, data);
      })
    );
  }

  // Run after DOM is ready
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', hydrate);
  } else {
    hydrate();
  }

  // Expose publicly for manual refresh
  window.CivicCMS = { hydrate, fetchPage };
})();
