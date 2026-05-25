/**
 * CivicCMS Admin Language Manager
 * Handles EN / தமிழ் / हिंदी for ALL admin pages
 * No page reload — live DOM swap + localStorage persist
 * Does NOT touch session/token — safe to call anytime
 */
const AdminLang = (() => {

  /* ── Translation table ─────────────────── */
  const T = {
    en: {
      code: 'en', flag: '🇬🇧', label: 'English', nativeLabel: 'English',
      hint: 'Default language',

      /* Sidebar */
      nav: {
        dashboard: 'Dashboard', complaints: 'Complaints', deptHeads: 'Dept Heads',
        alerts: 'Alerts', heatmap: 'Heatmap', contentEditor: 'Content Editor',
        viewSite: 'View Site', settings: 'Settings', language: 'Language',
        signOut: 'Sign Out', tools: 'TOOLS', account: 'ACCOUNT', main: 'MAIN',
      },

      /* Dashboard page */
      dash: {
        title: 'Dashboard',
        welcome: 'Welcome back',
        subtitle: 'AI-Powered Complaint Management',
        totalComplaints: 'Total Complaints',
        resolved: 'Resolved',
        critical: 'Critical',
        highUrgency: 'High Urgency',
        allTime: 'All time',
        closedIssues: 'Closed issues',
        immediateAction: 'Immediate action',
        within24h: 'Within 24 hours',
        recentActivity: 'Recent Activity',
        viewAll: 'View all →',
        quickStats: 'Quick Stats',
        resolutionRate: 'Resolution Rate',
        criticalRate: 'Critical Rate',
        pending: 'Pending',
        byCategory: '📊 Complaints by Category',
        byPriority: '🎯 Complaints by Priority',
        deptBreakdown: '🏢 Department-wise Breakdown',
        liveCount: 'Live count · auto-routed by AI',
        manage: 'Manage →',
        filter: 'Filter →',
        criticalHighPriority: '🚨 Critical & High Priority',
        noUrgent: 'No urgent complaints — all good! 🎉',
        noActivity: 'No recent activity',
        unassigned: 'complaints not assigned.',
        assignLink: 'Assign →',
      },

      /* Complaints page */
      comp: {
        title: 'Complaint Management',
        subtitle: 'View, filter by department, assign departments and update complaints',
        filterStatus: 'Filter by Status', filterDept: 'Filter by Department',
        filterPriority: 'Filter by Priority',
        allStatuses: 'All Statuses', allDepts: 'All Departments', allPriorities: 'All Priorities',
        filterBtn: '● Filter', resetBtn: 'Reset', csvBtn: '📥 CSV',
        showing: 'Showing', complaints: 'complaint(s)',
        colId: 'ID', colTitle: 'Title', colCategory: 'Category', colPriority: 'Priority',
        colStatus: 'Status', colSubmittedBy: 'Submitted By', colDept: 'Department',
        colSubmitted: 'Submitted', colActions: 'Actions',
        viewBtn: 'View', assignBtn: 'Assign Dept',
        loading: 'Loading complaints…', noComplaints: 'No complaints found',
        statusSaved: 'Status updated successfully',
        deptAssigned: 'Department assigned successfully',
        unassigned: 'Unassigned',
      },

      /* Dept heads page */
      dept: {
        title: 'Department Heads',
        subtitle: 'Manage department head accounts and their assignments',
      },

      /* Alerts page */
      alert: {
        title: 'Alerts & Escalations',
        subtitle: 'Critical and high-priority complaints requiring immediate attention',
      },

      /* Shared table/badge strings */
      status: {
        OPEN: 'Open', ASSIGNED: 'Assigned', IN_PROGRESS: 'In Progress',
        RESOLVED: 'Resolved', ESCALATED: 'Escalated', PENDING: 'Pending',
      },
      priority: { CRITICAL: 'Critical', HIGH: 'High', MEDIUM: 'Medium', LOW: 'Low' },
      time: { justNow: 'just now', minAgo: 'min ago', hrAgo: 'hr ago', daysAgo: 'days ago' },
    },

    ta: {
      code: 'ta', flag: '🇮🇳', label: 'Tamil', nativeLabel: 'தமிழ்',
      hint: 'தமிழ்நாடு · India',

      nav: {
        dashboard: 'டாஷ்போர்டு', complaints: 'புகார்கள்', deptHeads: 'துறை தலைவர்கள்',
        alerts: 'எச்சரிக்கைகள்', heatmap: 'வெப்பமண்டலம்', contentEditor: 'உள்ளடக்க ஆசிரியர்',
        viewSite: 'தளம் பார்க்க', settings: 'அமைப்புகள்', language: 'மொழி',
        signOut: 'வெளியேறு', tools: 'கருவிகள்', account: 'கணக்கு', main: 'முகப்பு',
      },

      dash: {
        title: 'டாஷ்போர்டு',
        welcome: 'வரவேற்கிறோம்',
        subtitle: 'AI இயக்கும் புகார் மேலாண்மை',
        totalComplaints: 'மொத்த புகார்கள்',
        resolved: 'தீர்க்கப்பட்டவை',
        critical: 'தீவிரமானவை',
        highUrgency: 'அதிக அவசரம்',
        allTime: 'மொத்தம்',
        closedIssues: 'மூடப்பட்ட சிக்கல்கள்',
        immediateAction: 'உடனடி நடவடிக்கை',
        within24h: '24 மணி நேரத்தில்',
        recentActivity: 'சமீபத்திய செயல்கள்',
        viewAll: 'அனைத்தும் பார்க்க →',
        quickStats: 'விரைவு புள்ளிவிவரங்கள்',
        resolutionRate: 'தீர்வு விகிதம்',
        criticalRate: 'தீவிர விகிதம்',
        pending: 'நிலுவையில்',
        byCategory: '📊 வகைப்படி புகார்கள்',
        byPriority: '🎯 முன்னுரிமைப்படி புகார்கள்',
        deptBreakdown: '🏢 துறைவாரி விவரம்',
        liveCount: 'நேரடி எண்ணிக்கை · AI மூலம் திசைதிருப்பப்பட்டது',
        manage: 'நிர்வகிக்க →',
        filter: 'வடிகட்ட →',
        criticalHighPriority: '🚨 தீவிர & உயர் முன்னுரிமை',
        noUrgent: 'அவசர புகார்கள் இல்லை — நல்லது! 🎉',
        noActivity: 'சமீபத்திய செயல் இல்லை',
        unassigned: 'புகார்கள் ஒதுக்கப்படவில்லை.',
        assignLink: 'ஒதுக்கவும் →',
      },

      comp: {
        title: 'புகார் மேலாண்மை',
        subtitle: 'துறைவாரி வடிகட்டல், ஒதுக்கீடு மற்றும் புகார் மேலாண்மை',
        filterStatus: 'நிலை வடிகட்டு', filterDept: 'துறை வடிகட்டு',
        filterPriority: 'முன்னுரிமை வடிகட்டு',
        allStatuses: 'அனைத்து நிலைகளும்', allDepts: 'அனைத்து துறைகளும்', allPriorities: 'அனைத்து முன்னுரிமைகளும்',
        filterBtn: '● வடிகட்டு', resetBtn: 'மீட்டமை', csvBtn: '📥 CSV',
        showing: 'காட்டுகிறது', complaints: 'புகார்கள்',
        colId: 'அடையாளம்', colTitle: 'தலைப்பு', colCategory: 'வகை', colPriority: 'முன்னுரிமை',
        colStatus: 'நிலை', colSubmittedBy: 'சமர்பித்தவர்', colDept: 'துறை',
        colSubmitted: 'சமர்பித்த தேதி', colActions: 'செயல்கள்',
        viewBtn: 'பார்க்க', assignBtn: 'துறை ஒதுக்கு',
        loading: 'புகார்கள் ஏற்றுகிறது…', noComplaints: 'புகார்கள் இல்லை',
        statusSaved: 'நிலை வெற்றிகரமாக புதுப்பிக்கப்பட்டது',
        deptAssigned: 'துறை வெற்றிகரமாக ஒதுக்கப்பட்டது',
        unassigned: 'ஒதுக்கப்படவில்லை',
      },

      dept: {
        title: 'துறை தலைவர்கள்',
        subtitle: 'துறை தலைவர் கணக்குகள் மற்றும் நியமனங்களை நிர்வகிக்கவும்',
      },

      alert: {
        title: 'எச்சரிக்கைகள் & தீவிரப்படுத்தல்',
        subtitle: 'உடனடி கவனம் தேவைப்படும் தீவிர புகார்கள்',
      },

      status: {
        OPEN: 'திறந்திருக்கிறது', ASSIGNED: 'ஒதுக்கப்பட்டது', IN_PROGRESS: 'செயலில்',
        RESOLVED: 'தீர்க்கப்பட்டது', ESCALATED: 'தீவிரப்படுத்தப்பட்டது', PENDING: 'நிலுவையில்',
      },
      priority: { CRITICAL: 'தீவிரம்', HIGH: 'உயர்', MEDIUM: 'நடுத்தர', LOW: 'குறைந்த' },
      time: { justNow: 'இப்போதுதான்', minAgo: 'நிமிடம் முன்பு', hrAgo: 'மணி முன்பு', daysAgo: 'நாட்கள் முன்பு' },
    },

    hi: {
      code: 'hi', flag: '🇮🇳', label: 'Hindi', nativeLabel: 'हिंदी',
      hint: 'भारत · India',

      nav: {
        dashboard: 'डैशबोर्ड', complaints: 'शिकायतें', deptHeads: 'विभाग प्रमुख',
        alerts: 'अलर्ट', heatmap: 'हीटमैप', contentEditor: 'सामग्री संपादक',
        viewSite: 'साइट देखें', settings: 'सेटिंग्स', language: 'भाषा',
        signOut: 'साइन आउट', tools: 'उपकरण', account: 'खाता', main: 'मुख्य',
      },

      dash: {
        title: 'डैशबोर्ड',
        welcome: 'वापस स्वागत है',
        subtitle: 'AI-संचालित शिकायत प्रबंधन',
        totalComplaints: 'कुल शिकायतें',
        resolved: 'हल की गईं',
        critical: 'गंभीर',
        highUrgency: 'उच्च तात्कालिकता',
        allTime: 'कुल समय',
        closedIssues: 'बंद मुद्दे',
        immediateAction: 'तुरंत कार्रवाई',
        within24h: '24 घंटों में',
        recentActivity: 'हालिया गतिविधि',
        viewAll: 'सभी देखें →',
        quickStats: 'त्वरित आँकड़े',
        resolutionRate: 'समाधान दर',
        criticalRate: 'गंभीर दर',
        pending: 'लंबित',
        byCategory: '📊 श्रेणी अनुसार शिकायतें',
        byPriority: '🎯 प्राथमिकता अनुसार शिकायतें',
        deptBreakdown: '🏢 विभाग-वार विवरण',
        liveCount: 'लाइव गिनती · AI द्वारा स्वतः निर्देशित',
        manage: 'प्रबंधित करें →',
        filter: 'फ़िल्टर करें →',
        criticalHighPriority: '🚨 गंभीर & उच्च प्राथमिकता',
        noUrgent: 'कोई जरूरी शिकायत नहीं — सब ठीक है! 🎉',
        noActivity: 'कोई हालिया गतिविधि नहीं',
        unassigned: 'शिकायतें असाइन नहीं हुईं।',
        assignLink: 'असाइन करें →',
      },

      comp: {
        title: 'शिकायत प्रबंधन',
        subtitle: 'विभाग द्वारा फ़िल्टर करें, विभाग असाइन करें और शिकायतें अपडेट करें',
        filterStatus: 'स्थिति फ़िल्टर', filterDept: 'विभाग फ़िल्टर',
        filterPriority: 'प्राथमिकता फ़िल्टर',
        allStatuses: 'सभी स्थितियां', allDepts: 'सभी विभाग', allPriorities: 'सभी प्राथमिकताएं',
        filterBtn: '● फ़िल्टर', resetBtn: 'रीसेट', csvBtn: '📥 CSV',
        showing: 'दिखा रहा है', complaints: 'शिकायत(ें)',
        colId: 'आईडी', colTitle: 'शीर्षक', colCategory: 'श्रेणी', colPriority: 'प्राथमिकता',
        colStatus: 'स्थिति', colSubmittedBy: 'द्वारा सबमिट', colDept: 'विभाग',
        colSubmitted: 'सबमिट किया', colActions: 'कार्रवाई',
        viewBtn: 'देखें', assignBtn: 'विभाग असाइन',
        loading: 'शिकायतें लोड हो रही हैं…', noComplaints: 'कोई शिकायत नहीं मिली',
        statusSaved: 'स्थिति सफलतापूर्वक अपडेट हुई',
        deptAssigned: 'विभाग सफलतापूर्वक असाइन हुआ',
        unassigned: 'असाइन नहीं किया',
      },

      dept: {
        title: 'विभाग प्रमुख',
        subtitle: 'विभाग प्रमुख खातों और उनके असाइनमेंट का प्रबंधन करें',
      },

      alert: {
        title: 'अलर्ट और एस्केलेशन',
        subtitle: 'तुरंत ध्यान देने की आवश्यकता वाली गंभीर शिकायतें',
      },

      status: {
        OPEN: 'खुली', ASSIGNED: 'असाइन की गई', IN_PROGRESS: 'प्रगति में',
        RESOLVED: 'हल हो गई', ESCALATED: 'एस्केलेट हुई', PENDING: 'लंबित',
      },
      priority: { CRITICAL: 'गंभीर', HIGH: 'उच्च', MEDIUM: 'मध्यम', LOW: 'कम' },
      time: { justNow: 'अभी-अभी', minAgo: 'मिनट पहले', hrAgo: 'घंटे पहले', daysAgo: 'दिन पहले' },
    },
  };

  /* ── Core helpers ─────────────────────────── */
  const STORAGE_KEY = 'civic-lang';

  function getCode()    { return localStorage.getItem(STORAGE_KEY) || 'en'; }
  function get()        { return T[getCode()] || T.en; }
  function all()        { return Object.values(T); }

  /**
   * Set language — NO page reload, NO session touch, just DOM swap
   * @param {string} code  'en' | 'ta' | 'hi'
   */
  function set(code) {
    if (!T[code]) return;
    localStorage.setItem(STORAGE_KEY, code);
    applyToPage();
    return T[code];
  }

  /**
   * Apply translations to every element with data-i18n="key.subkey"
   * e.g.  <span data-i18n="dash.title"></span>
   * Also updates the sidebar language button label
   */
  function applyToPage() {
    const lang = get();

    /* 1. data-i18n elements */
    document.querySelectorAll('[data-i18n]').forEach(el => {
      const key = el.getAttribute('data-i18n');            // e.g. "dash.title"
      const parts = key.split('.');
      let val = lang;
      for (const p of parts) { val = val?.[p]; }
      if (typeof val === 'string') el.textContent = val;
    });

    /* 2. data-i18n-ph (placeholder) */
    document.querySelectorAll('[data-i18n-ph]').forEach(el => {
      const key = el.getAttribute('data-i18n-ph');
      const parts = key.split('.');
      let val = lang;
      for (const p of parts) { val = val?.[p]; }
      if (typeof val === 'string') el.placeholder = val;
    });

    /* 3. Sidebar language button */
    const lbl = document.getElementById('sb-lang-label');
    if (lbl) lbl.textContent = lang.flag + ' ' + lang.nativeLabel;

    /* 4. html lang attribute */
    document.documentElement.lang = lang.code;
  }

  /* Auto-apply on load */
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', applyToPage);
  } else {
    applyToPage();
  }

  return { get, getCode, set, all, applyToPage, T };
})();
