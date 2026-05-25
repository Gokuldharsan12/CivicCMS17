/**
 * CivicCMS — Global Language Manager
 * Full translation for EN / தமிழ் / हिंदी
 */
const LangManager = (() => {

  const LANGS = {
    en: {
      code:'en', voiceCode:'en-IN', label:'English', flag:'🇬🇧',
      nav:{ home:'Home', submit:'Submit', track:'Track', myComplaints:'My Complaints', admin:'Admin', logout:'Logout', settings:'Settings', language:'Language' },
      hero:{ eyebrow:'AI-Powered Platform — Live', h1:'Multimodal <span class="accent">Civic</span> Issue Intelligence<br>for Predictive Urban Service Insights<br>and Real-Time Governance Analytics', desc:'Report road damage, water leaks, electricity faults and more. Our AI instantly classifies, prioritises and routes your complaint to the right department.', btnSubmit:'📨 Submit a Complaint', btnTrack:'🔍 Track Your Complaint', f1:'AI Auto-Classification', f2:'Real-Time Status', f3:'Duplicate Detection', f4:'Risk Scoring Engine' },
      home:{ howTitle:'How It Works', howSub:'Three steps from report to resolution, powered by AI', s1t:'Submit', s1d:'Describe the issue, pin your location on the interactive map, and optionally attach a photo.', s2t:'AI Processes', s2d:'Our AI extracts keywords, detects duplicates, scores risk, and routes to the correct department.', s3t:'Get Resolved', s3d:'Track status in real time, receive email updates, and rate the resolution quality.', trackTitle:'🔍 Quick Track', trackPh:'Enter Complaint ID — e.g. 42', trackBtn:'Track', kpi1:'Total Complaints', kpi2:'Submitted Today', kpi3:'Escalated', kpi4:'Avg. Rating / 5' },
      submit:{ title:'📨 Submit a Complaint', sub:'Fill in the details below. Our AI will classify and route it automatically.', steps:['Details','Location','Photo','Review'], s1:'Step 1 — Complaint Details', s2:'Step 2 — Location', s3:'Step 3 — Photo', s4:'Step 4 — Review & Submit', step1:'Step 1 — Complaint Details', step2:'Step 2 — Location', step3:'Step 3 — Photo', step4:'Step 4 — Review & Submit', lblTitle:'Title *', lblCat:'Category *', lblDesc:'Description *', lblAddr:'Address', titlePh:'e.g. Large pothole on Main Street', descPh:'Describe the issue in detail — minimum 20 characters', catDefault:'— Select Category —', cats:['Roads','Sanitation','Electricity','Water','Public Safety','General'], mapHint:'Click on the map to drop a pin at the complaint location.', btnLoc:'🌍 Use My Location', btnLocation:'🌍 Use My Location', btnNext:'Next →', btnBack:'← Back', btnSubmit:'✅ Submit Complaint', optional:'(Optional)', dzLabel:'Click or drag & drop a photo', voiceTip:'💡 Tip: Click 🎙️ to speak your complaint instead of typing', listening:'Listening…', listeningText:'Listening…', successTitle:'Complaint Submitted!', successIdLabel:'Your Complaint ID is:', successNote:'Save this ID to track your complaint. You will also receive an email confirmation.', btnCopy:'📋 Copy ID', btnTrack:'🔍 Track This Complaint', btnAnother:'Submit Another', vTitle:'Title is required', vCat:'Please select a category', vDesc:'Description must be at least 20 characters', vLoc:'Please click on the map to set the location', vLogin:'You must be logged in to submit a complaint.', validTitle:'Title is required', validCat:'Please select a category', validDesc:'Description must be at least 20 characters', validLoc:'Please click on the map to set the location', validLogin:'You must be logged in to submit a complaint.', loginRequired:'You must be logged in to submit a complaint.', addrNotFound:'❌ Address not found. Try a more specific address or click the map.', addrFail:'❌ Search failed. Please click the map to set location.', locDenied:'🔒 Location permission denied. Click the 🔒 lock icon → Allow Location, then refresh.', locUnavail:'📡 Location unavailable — click the map to set your location.', locTimeout:'⏱️ Location timed out. Please try again or click the map.' },
      history:{ title:'📋 My Complaints', sub:'View and manage all your submitted complaints', noComplaints:'No complaints found', noComplaintsSub:'You haven\'t submitted any complaints yet.', btnSubmit:'📨 Submit Your First Complaint', loadError:'Could not load your complaints. Please try again.', filterAll:'All Statuses', track:'🔍 Track', rate:'⭐ Rate', overdue:'⚠️ Overdue', loginPromptTitle:'Sign in to view your complaints', loginPromptSub:'Create a free account to submit and track complaints.', btnLogin:'Sign In →', btnRegister:'Create Account →' },
      rate:{ title:'⭐ Rate the Resolution', sub:'Your feedback helps improve civic services for everyone.', yourFeedback:'Your Feedback', labelSpeed:'Speed of Resolution', labelQuality:'Quality of Resolution', labelComm:'Communication', labelComments:'Comments', commentsOptional:'(optional)', commentsPh:'Any additional feedback...', btnSubmit:'Submit Rating →', btnSubmitting:'Submitting...', btnBack:'Back to Home', errNoId:'No complaint ID provided.', errNotResolved:'This complaint is not yet resolved.', errNotFound:'Complaint not found.', errLoad:'Could not load complaint.', errRateAll:'Please rate all three categories.', alreadyRatedTitle:'Already Rated', alreadyRatedMsg:'You have already submitted a rating for this complaint. Thank you!', successTitle:'Thank You!', successMsg:'Your feedback has been recorded and will help improve our services.' },
      track:{ title:'🔍 Track Your Complaint', sub:'Enter your complaint ID to view real-time status and progress.', ph:'Enter Complaint ID — e.g. 42', btn:'Track', e1:'Please enter a Complaint ID', e2:'No complaint found with that ID. Please check and try again.', e3:'Could not load complaint. Please try again.', stage1:'Submitted', stage1d:'Complaint received by system', stage2:'AI Analysed', stage2d:'Keywords, risk and priority assessed', stage3:'Assigned to Dept.', stage3d:'Routed to ', stage4:'In Progress', stage4d:'Department is working on it', stage5:'Resolved', stage5d:'Issue resolved', stage6:'Rated', stage6d:'Citizen feedback submitted', progressTitle:'Complaint Progress', lblDept:'Department', lblSla:'SLA Deadline', resolutionStatus:'Resolution Status', complaintResolved:'Complaint Resolved', resolvedOn:'Resolved on', pendingAssignment:'Pending assignment', btnRate:'⭐ Rate this Resolution', resolutionNote:'✅ Resolution Note:', locationTitle:'📍 Complaint Location', overdueBy:'Overdue by', timeRemaining:'Time Remaining', dueWas:'Due was', slaBreach:'SLA breached', dueLabel:'Due', slaUsed:'of SLA used' },
      register:{ title:'Create Account', sub:'Join CivicCMS — Report issues, track progress', lblName:'Full Name', lblEmail:'Email Address', lblPass:'Password', passHint:'(min 6 characters)', namePh:'Your full name', emailPh:'your@email.com', passPh:'••••••••', btnCreate:'Create Account →', btnCreating:'Creating account...', hasAccount:'Already have an account?', signIn:'Sign in here →', e1:'All fields are required.', e2:'Password must be at least 6 characters.', e3:'Registration failed. Please try again.', success:'Account created! Please sign in to continue…' },
      login:{ sub:'Citizen Portal — Sign in or create an account', tabUser:'User Login', tabAdmin:'Admin Login', lblEmail:'Email Address', lblPass:'Password', forgotPass:'Forgot password?', btnSignIn:'Sign In →', orEmail:'or sign in with email', btnGoogle:'Continue with Google', btnPhone:'Continue with Phone Number', noAccount:"Don't have an account?", createAccount:'Create one here →', tabL:'Login', tabR:'Register', lblName:'Full Name', passHint:'(min 6 chars)', emailPh:'your@email.com', passPh:'••••••••', namePh:'Your full name', btnL:'Sign In →', btnR:'Create Account →', e1:'Email and password are required.', e2:'All fields are required.', e3:'Password must be at least 6 characters.', e4:'Invalid credentials.', e5:'Registration failed.', fpTitle:'Reset Password', fpStep1Sub:'Enter your registered email address.', fpEmailLbl:'Email Address', fpContinue:'Continue →', fpStep2Sub:'Choose a strong new password for', fpNewPass:'New Password', fpConfPass:'Confirm Password', fpReset:'Reset Password →', fpSuccessTitle:'Password Reset!', fpSuccessSub:'Your password has been updated. You can now sign in.', fpGoLogin:'Go to Sign In →', fpBack:'← Back', fpCancel:'Cancel', phoneStep1:'Enter your 10-digit mobile number to receive an OTP.', phonePh:'10-digit number', btnSendOtp:'Send OTP →', btnVerifyOtp:'Verify & Sign In →' }
    },
    ta: {
      code:'ta', voiceCode:'ta-IN', label:'தமிழ்', flag:'🇮🇳',
      nav:{ home:'முகப்பு', submit:'சமர்ப்பிக்கவும்', track:'கண்காணிக்கவும்', myComplaints:'என் புகார்கள்', admin:'நிர்வாகி', logout:'வெளியேறு', settings:'அமைப்புகள்', language:'மொழி' },
      hero:{ eyebrow:'AI இயக்கப்படும் தளம் — நேரடி', h1:'பன்முக <span class="accent">குடிமக்கள்</span> சிக்கல் நுண்ணறிவு<br>முன்கணிப்பு நகர்ப்புற சேவை நுண்ணறிவுகளுக்காக<br>மற்றும் நிகழ்நேர நகராட்சி பகுப்பாய்வு', desc:'சாலை சேதம், நீர் கசிவு, மின்சார கோளாறுகள் மற்றும் பலவற்றை புகாரளிக்கவும். எங்கள் AI உடனடியாக வகைப்படுத்தி, முன்னுரிமை அளித்து, சரியான துறைக்கு அனுப்புகிறது.', btnSubmit:'📨 புகார் சமர்ப்பிக்கவும்', btnTrack:'🔍 புகாரை கண்காணிக்கவும்', f1:'AI தானியங்கி வகைப்படுத்தல்', f2:'நிகழ்நேர நிலை', f3:'நகல் கண்டறிதல்', f4:'ஆபத்து மதிப்பீட்டு என்ஜின்' },
      home:{ howTitle:'எப்படி செயல்படுகிறது', howSub:'AI இயக்கப்படும் மூன்று படிகளில் தீர்வு', s1t:'சமர்ப்பிக்கவும்', s1d:'சிக்கலை விவரிக்கவும், வரைபடத்தில் இடத்தை குறிக்கவும், புகைப்படம் இணைக்கவும்.', s2t:'AI செயலாக்கம்', s2d:'எங்கள் AI முக்கியசொற்களை பிரித்தெடுக்கிறது, நகல்களை கண்டறிகிறது, ஆபத்தை மதிப்பிடுகிறது.', s3t:'தீர்வு பெறுங்கள்', s3d:'நிகழ்நேரத்தில் நிலையை கண்காணிக்கவும், மின்னஞ்சல் புதுப்பிப்புகள் பெறவும்.', trackTitle:'🔍 விரைவு கண்காணிப்பு', trackPh:'புகார் ஐடி உள்ளிடவும் — எ.கா. 42', trackBtn:'கண்காணிக்கவும்', kpi1:'மொத்த புகார்கள்', kpi2:'இன்று சமர்ப்பிக்கப்பட்டவை', kpi3:'தீவிரப்படுத்தப்பட்டவை', kpi4:'சராசரி மதிப்பீடு / 5' },
      submit:{ title:'📨 புகார் சமர்ப்பிக்கவும்', sub:'கீழே விவரங்களை நிரப்பவும். எங்கள் AI தானாக வகைப்படுத்தும்.', steps:['விவரங்கள்','இருப்பிடம்','புகைப்படம்','மதிப்பாய்வு'], s1:'படி 1 — புகார் விவரங்கள்', s2:'படி 2 — இருப்பிடம்', s3:'படி 3 — புகைப்படம்', s4:'படி 4 — மதிப்பாய்வு & சமர்ப்பிக்கவும்', step1:'படி 1 — புகார் விவரங்கள்', step2:'படி 2 — இருப்பிடம்', step3:'படி 3 — புகைப்படம்', step4:'படி 4 — மதிப்பாய்வு & சமர்ப்பிக்கவும்', lblTitle:'தலைப்பு *', lblCat:'வகை *', lblDesc:'விளக்கம் *', lblAddr:'முகவரி', titlePh:'எ.கா. பிரதான தெருவில் பெரிய குழி', descPh:'சிக்கலை விரிவாக விவரிக்கவும் — குறைந்தபட்சம் 20 எழுத்துக்கள்', catDefault:'— வகையை தேர்வு செய்யவும் —', cats:['சாலை','சுகாதாரம்','மின்சாரம்','நீர்','பொது பாதுகாப்பு','பொது'], mapHint:'புகார் இருப்பிடத்தை குறிக்க வரைபடத்தில் கிளிக் செய்யவும்.', btnLoc:'🌍 என் இருப்பிடத்தை பயன்படுத்து', btnLocation:'🌍 என் இருப்பிடத்தை பயன்படுத்து', btnNext:'அடுத்து →', btnBack:'← பின்', btnSubmit:'✅ புகாரை சமர்ப்பிக்கவும்', optional:'(விருப்பமானது)', dzLabel:'கிளிக் செய்யவும் அல்லது புகைப்படத்தை இழுத்து விடவும்', voiceTip:'💡 குறிப்பு: தட்டச்சு செய்வதற்கு பதிலாக பேச 🎙️ கிளிக் செய்யவும்', listening:'கேட்கிறது…', listeningText:'கேட்கிறது…', successTitle:'புகார் சமர்ப்பிக்கப்பட்டது!', successIdLabel:'உங்கள் புகார் ஐடி:', successNote:'உங்கள் புகாரை கண்காணிக்க இந்த ஐடியை சேமிக்கவும். மின்னஞ்சல் உறுதிப்படுத்தல் வரும்.', btnCopy:'📋 ஐடி நகலெடு', btnTrack:'🔍 இந்த புகாரை கண்காணிக்கவும்', btnAnother:'மற்றொரு புகார் சமர்ப்பிக்கவும்', vTitle:'தலைப்பு தேவை', vCat:'தயவுசெய்து வகையை தேர்வு செய்யவும்', vDesc:'விளக்கம் குறைந்தது 20 எழுத்துக்கள் இருக்க வேண்டும்', vLoc:'இருப்பிடத்தை அமைக்க வரைபடத்தில் கிளிக் செய்யவும்', vLogin:'புகார் சமர்ப்பிக்க நீங்கள் உள்நுழைந்திருக்க வேண்டும்.', validTitle:'தலைப்பு தேவை', validCat:'தயவுசெய்து வகையை தேர்வு செய்யவும்', validDesc:'விளக்கம் குறைந்தது 20 எழுத்துக்கள் இருக்க வேண்டும்', validLoc:'இருப்பிடத்தை அமைக்க வரைபடத்தில் கிளிக் செய்யவும்', validLogin:'புகார் சமர்ப்பிக்க நீங்கள் உள்நுழைந்திருக்க வேண்டும்.', loginRequired:'புகார் சமர்ப்பிக்க நீங்கள் உள்நுழைந்திருக்க வேண்டும்.', addrNotFound:'❌ முகவரி கிடைக்கவில்லை. மிகவும் குறிப்பிட்ட முகவரி முயற்சிக்கவும் அல்லது வரைபடத்தில் கிளிக் செய்யவும்.', addrFail:'❌ தேடல் தோல்வியடைந்தது. இருப்பிடத்தை அமைக்க வரைபடத்தில் கிளிக் செய்யவும்.', locDenied:'🔒 இருப்பிட அனுமதி மறுக்கப்பட்டது. 🔒 ஐகானை கிளிக் → இருப்பிடத்தை அனுமதிக்கவும்.', locUnavail:'📡 இருப்பிடம் கிடைக்கவில்லை — வரைபடத்தில் கிளிக் செய்யவும்.', locTimeout:'⏱️ இருப்பிட கோரிக்கை நேரம் முடிந்தது. மீண்டும் முயற்சிக்கவும்.' },
      history:{ title:'📋 என் புகார்கள்', sub:'சமர்ப்பிக்கப்பட்ட அனைத்து புகார்களையும் பார்க்கவும்', noComplaints:'புகார்கள் இல்லை', noComplaintsSub:'நீங்கள் இன்னும் எந்த புகாரையும் சமர்ப்பிக்கவில்லை.', btnSubmit:'📨 முதல் புகாரை சமர்ப்பிக்கவும்', loadError:'புகார்களை ஏற்ற முடியவில்லை. மீண்டும் முயற்சிக்கவும்.', filterAll:'அனைத்து நிலைகளும்', track:'🔍 கண்காணிக்கவும்', rate:'⭐ மதிப்பிடவும்', overdue:'⚠️ தாமதமானது', loginPromptTitle:'புகார்களை பார்க்க உள்நுழையவும்', loginPromptSub:'புகார்களை சமர்ப்பிக்க இலவச கணக்கை உருவாக்கவும்.', btnLogin:'உள்நுழைக →', btnRegister:'கணக்கு உருவாக்கவும் →' },
      rate:{ title:'⭐ தீர்வை மதிப்பிடவும்', sub:'உங்கள் கருத்து குடிமக்கள் சேவைகளை மேம்படுத்த உதவுகிறது.', yourFeedback:'உங்கள் கருத்து', labelSpeed:'தீர்வின் வேகம்', labelQuality:'தீர்வின் தரம்', labelComm:'தகவல் தொடர்பு', labelComments:'கருத்துகள்', commentsOptional:'(விருப்பமானது)', commentsPh:'கூடுதல் கருத்துக்கள்...', btnSubmit:'மதிப்பீட்டை சமர்ப்பிக்கவும் →', btnSubmitting:'சமர்ப்பிக்கிறது...', btnBack:'முகப்புக்கு திரும்பு', errNoId:'புகார் ஐடி வழங்கப்படவில்லை.', errNotResolved:'இந்த புகார் இன்னும் தீர்க்கப்படவில்லை.', errNotFound:'புகார் கிடைக்கவில்லை.', errLoad:'புகாரை ஏற்ற முடியவில்லை.', errRateAll:'மூன்று வகைகளையும் மதிப்பிடவும்.', alreadyRatedTitle:'ஏற்கனவே மதிப்பிடப்பட்டது', alreadyRatedMsg:'நீங்கள் ஏற்கனவே இந்த புகாருக்கு மதிப்பீடு சமர்ப்பித்துள்ளீர்கள். நன்றி!', successTitle:'நன்றி!', successMsg:'உங்கள் கருத்து பதிவு செய்யப்பட்டுள்ளது.' },
      track:{ title:'🔍 உங்கள் புகாரை கண்காணிக்கவும்', sub:'நிகழ்நேர நிலையை பார்க்க புகார் ஐடி உள்ளிடவும்.', ph:'புகார் ஐடி உள்ளிடவும் — எ.கா. 42', btn:'கண்காணிக்கவும்', e1:'புகார் ஐடி உள்ளிடவும்', e2:'அந்த ஐடியில் புகார் இல்லை. சரிபார்த்து மீண்டும் முயற்சிக்கவும்.', e3:'புகாரை ஏற்ற முடியவில்லை. மீண்டும் முயற்சிக்கவும்.', stage1:'சமர்ப்பிக்கப்பட்டது', stage1d:'புகார் பெறப்பட்டது', stage2:'AI பகுப்பாய்வு', stage2d:'முக்கியசொற்கள், ஆபத்து மதிப்பிடப்பட்டது', stage3:'துறைக்கு ஒதுக்கப்பட்டது', stage3d:'அனுப்பப்பட்டது: ', stage4:'நடவடிக்கையில்', stage4d:'துறை பணிபுரிகிறது', stage5:'தீர்க்கப்பட்டது', stage5d:'சிக்கல் தீர்க்கப்பட்டது', stage6:'மதிப்பிடப்பட்டது', stage6d:'குடிமக்கள் கருத்து சமர்ப்பிக்கப்பட்டது', progressTitle:'புகார் முன்னேற்றம்', lblDept:'துறை', lblSla:'SLA காலக்கெடு', resolutionStatus:'தீர்வு நிலை', complaintResolved:'புகார் தீர்க்கப்பட்டது', resolvedOn:'தீர்க்கப்பட்ட தேதி', pendingAssignment:'ஒதுக்கீடு நிலுவையில்', btnRate:'⭐ தீர்வை மதிப்பிடவும்', resolutionNote:'✅ தீர்வு குறிப்பு:', locationTitle:'📍 புகார் இடம்', overdueBy:'தாமதம்', timeRemaining:'மீதமுள்ள நேரம்', dueWas:'காலக்கெடு', slaBreach:'SLA மீறல்', dueLabel:'காலக்கெடு', slaUsed:'SLA பயன்படுத்தப்பட்டது' },
      register:{ title:'கணக்கு உருவாக்கவும்', sub:'CivicCMS இல் சேரவும் — சிக்கல்களை புகாரளிக்கவும், முன்னேற்றத்தை கண்காணிக்கவும்', lblName:'முழு பெயர்', lblEmail:'மின்னஞ்சல் முகவரி', lblPass:'கடவுச்சொல்', passHint:'(குறைந்தது 6 எழுத்துக்கள்)', namePh:'உங்கள் முழு பெயர்', emailPh:'உங்கள்@மின்னஞ்சல்.com', passPh:'••••••••', btnCreate:'கணக்கு உருவாக்கவும் →', btnCreating:'கணக்கு உருவாக்கப்படுகிறது...', hasAccount:'ஏற்கனவே கணக்கு உள்ளதா?', signIn:'இங்கே உள்நுழையவும் →', e1:'அனைத்து புலங்களும் தேவை.', e2:'கடவுச்சொல் குறைந்தது 6 எழுத்துக்கள் இருக்க வேண்டும்.', e3:'பதிவு தோல்வியடைந்தது. மீண்டும் முயற்சிக்கவும்.', success:'கணக்கு உருவாக்கப்பட்டது! தொடர உள்நுழையவும்…' },
      login:{ sub:'குடிமக்கள் இணையதளம் — உள்நுழைக அல்லது கணக்கு உருவாக்கவும்', tabUser:'பயனர் உள்நுழைவு', tabAdmin:'நிர்வாக உள்நுழைவு', lblEmail:'மின்னஞ்சல் முகவரி', lblPass:'கடவுச்சொல்', forgotPass:'கடவுச்சொல் மறந்துவிட்டீர்களா?', btnSignIn:'உள்நுழைக →', orEmail:'அல்லது மின்னஞ்சலில் உள்நுழைக', btnGoogle:'Google மூலம் தொடரவும்', btnPhone:'தொலைபேசி எண் மூலம் தொடரவும்', noAccount:'கணக்கு இல்லையா?', createAccount:'இங்கே உருவாக்கவும் →', tabL:'உள்நுழைவு', tabR:'பதிவு செய்யவும்', lblName:'முழு பெயர்', passHint:'(குறைந்தது 6 எழுத்துக்கள்)', emailPh:'உங்கள்@மின்னஞ்சல்.com', passPh:'••••••••', namePh:'உங்கள் முழு பெயர்', btnL:'உள்நுழைக →', btnR:'கணக்கு உருவாக்கவும் →', e1:'மின்னஞ்சல் மற்றும் கடவுச்சொல் தேவை.', e2:'அனைத்து புலங்களும் தேவை.', e3:'கடவுச்சொல் குறைந்தது 6 எழுத்துக்கள் இருக்க வேண்டும்.', e4:'தவறான நற்சான்றிதழ்கள்.', e5:'பதிவு தோல்வியடைந்தது.', fpTitle:'கடவுச்சொல் மீட்டமை', fpStep1Sub:'உங்கள் பதிவு செய்த மின்னஞ்சலை உள்ளிடவும்.', fpEmailLbl:'மின்னஞ்சல் முகவரி', fpContinue:'தொடரவும் →', fpStep2Sub:'இதற்கான புதிய கடவுச்சொல் தேர்வு செய்யவும்', fpNewPass:'புதிய கடவுச்சொல்', fpConfPass:'கடவுச்சொல் உறுதிப்படுத்தவும்', fpReset:'கடவுச்சொல் மீட்டமை →', fpSuccessTitle:'கடவுச்சொல் மீட்டமைக்கப்பட்டது!', fpSuccessSub:'உங்கள் கடவுச்சொல் புதுப்பிக்கப்பட்டது. இப்போது உள்நுழையலாம்.', fpGoLogin:'உள்நுழைவுக்கு செல்லவும் →', fpBack:'← பின்', fpCancel:'ரத்து செய்', phoneStep1:'OTP பெற உங்கள் 10 இலக்க மொபைல் எண்ணை உள்ளிடவும்.', phonePh:'10 இலக்க எண்', btnSendOtp:'OTP அனுப்பவும் →', btnVerifyOtp:'சரிபார்க்கவும் & உள்நுழைக →' }
    },
    hi: {
      code:'hi', voiceCode:'hi-IN', label:'हिंदी', flag:'🇮🇳',
      nav:{ home:'होम', submit:'सबमिट करें', track:'ट्रैक करें', myComplaints:'मेरी शिकायतें', admin:'एडमिन', logout:'लॉगआउट', settings:'सेटिंग्स', language:'भाषा' },
      hero:{ eyebrow:'AI-संचालित प्लेटफॉर्म — लाइव', h1:'बहुमोडल <span class="accent">नागरिक</span> समस्या बुद्धिमत्ता<br>पूर्वानुमानित शहरी सेवा अंतर्दृष्टि के लिए<br>और रीयल-टाइम शासन विश्लेषण', desc:'सड़क क्षति, पानी की लीकेज, बिजली की खराबी और अधिक की रिपोर्ट करें। हमारा AI तुरंत वर्गीकृत करता है, प्राथमिकता देता है और सही विभाग को भेजता है।', btnSubmit:'📨 शिकायत सबमिट करें', btnTrack:'🔍 शिकायत ट्रैक करें', f1:'AI ऑटो-वर्गीकरण', f2:'रीयल-टाइम स्थिति', f3:'डुप्लिकेट पहचान', f4:'जोखिम स्कोरिंग इंजन' },
      home:{ howTitle:'यह कैसे काम करता है', howSub:'AI द्वारा तीन चरणों में रिपोर्ट से समाधान तक', s1t:'सबमिट करें', s1d:'समस्या का वर्णन करें, मानचित्र पर स्थान पिन करें, और वैकल्पिक रूप से फ़ोटो संलग्न करें।', s2t:'AI प्रोसेसिंग', s2d:'हमारा AI कीवर्ड निकालता है, डुप्लिकेट का पता लगाता है, जोखिम स्कोर करता है।', s3t:'समाधान पाएं', s3d:'रीयल टाइम में स्थिति ट्रैक करें, ईमेल अपडेट प्राप्त करें और रेटिंग दें।', trackTitle:'🔍 त्वरित ट्रैक', trackPh:'शिकायत ID दर्ज करें — जैसे 42', trackBtn:'ट्रैक करें', kpi1:'कुल शिकायतें', kpi2:'आज सबमिट', kpi3:'एस्केलेटेड', kpi4:'औसत रेटिंग / 5' },
      submit:{ title:'📨 शिकायत सबमिट करें', sub:'नीचे विवरण भरें। हमारा AI स्वतः वर्गीकृत और रूट करेगा।', steps:['विवरण','स्थान','फ़ोटो','समीक्षा'], s1:'चरण 1 — शिकायत विवरण', s2:'चरण 2 — स्थान', s3:'चरण 3 — फ़ोटो', s4:'चरण 4 — समीक्षा और सबमिट', step1:'चरण 1 — शिकायत विवरण', step2:'चरण 2 — स्थान', step3:'चरण 3 — फ़ोटो', step4:'चरण 4 — समीक्षा और सबमिट', lblTitle:'शीर्षक *', lblCat:'श्रेणी *', lblDesc:'विवरण *', lblAddr:'पता', titlePh:'उदा. मुख्य सड़क पर बड़ा गड्ढा', descPh:'समस्या का विस्तार से वर्णन करें — न्यूनतम 20 अक्षर', catDefault:'— श्रेणी चुनें —', cats:['सड़क','स्वच्छता','बिजली','पानी','सार्वजनिक सुरक्षा','सामान्य'], mapHint:'शिकायत स्थान पर पिन लगाने के लिए मानचित्र पर क्लिक करें।', btnLoc:'🌍 मेरा स्थान उपयोग करें', btnLocation:'🌍 मेरा स्थान उपयोग करें', btnNext:'अगला →', btnBack:'← वापस', btnSubmit:'✅ शिकायत सबमिट करें', optional:'(वैकल्पिक)', dzLabel:'क्लिक करें या फ़ोटो खींचें और छोड़ें', voiceTip:'💡 टिप: टाइप करने की बजाय बोलने के लिए 🎙️ क्लिक करें', listening:'सुन रहा है…', listeningText:'सुन रहा है…', successTitle:'शिकायत सबमिट हो गई!', successIdLabel:'आपकी शिकायत ID है:', successNote:'अपनी शिकायत ट्रैक करने के लिए यह ID सहेजें। ईमेल पुष्टि भी आएगी।', btnCopy:'📋 ID कॉपी करें', btnTrack:'🔍 इस शिकायत को ट्रैक करें', btnAnother:'एक और सबमिट करें', vTitle:'शीर्षक आवश्यक है', vCat:'कृपया एक श्रेणी चुनें', vDesc:'विवरण कम से कम 20 अक्षर होना चाहिए', vLoc:'कृपया स्थान सेट करने के लिए मानचित्र पर क्लिक करें', vLogin:'शिकायत सबमिट करने के लिए लॉग इन करें।', validTitle:'शीर्षक आवश्यक है', validCat:'कृपया एक श्रेणी चुनें', validDesc:'विवरण कम से कम 20 अक्षर होना चाहिए', validLoc:'कृपया स्थान सेट करने के लिए मानचित्र पर क्लिक करें', validLogin:'शिकायत सबमिट करने के लिए लॉग इन करें।', loginRequired:'शिकायत सबमिट करने के लिए लॉग इन करें।', addrNotFound:'❌ पता नहीं मिला। अधिक विशिष्ट पता आज़माएं या मानचित्र पर क्लिक करें।', addrFail:'❌ खोज विफल। स्थान सेट करने के लिए मानचित्र पर क्लिक करें।', locDenied:'🔒 स्थान अनुमति अस्वीकृत। 🔒 आइकन → स्थान अनुमति दें, फिर रिफ्रेश करें।', locUnavail:'📡 स्थान अनुपलब्ध — मानचित्र पर क्लिक करके स्थान सेट करें।', locTimeout:'⏱️ स्थान अनुरोध समय समाप्त। पुनः प्रयास करें या मानचित्र पर क्लिक करें।' },
      history:{ title:'📋 मेरी शिकायतें', sub:'सबमिट की गई सभी शिकायतें देखें', noComplaints:'कोई शिकायत नहीं मिली', noComplaintsSub:'आपने अभी तक कोई शिकायत सबमिट नहीं की है।', btnSubmit:'📨 पहली शिकायत सबमिट करें', loadError:'शिकायतें लोड नहीं हो सकीं। पुनः प्रयास करें।', filterAll:'सभी स्थिति', track:'🔍 ट्रैक करें', rate:'⭐ रेट करें', overdue:'⚠️ अतिदेय', loginPromptTitle:'शिकायतें देखने के लिए साइन इन करें', loginPromptSub:'शिकायतें सबमिट और ट्रैक करने के लिए मुफ्त खाता बनाएं।', btnLogin:'साइन इन करें →', btnRegister:'खाता बनाएं →' },
      rate:{ title:'⭐ समाधान को रेट करें', sub:'आपकी प्रतिक्रिया नागरिक सेवाओं को बेहतर बनाने में मदद करती है।', yourFeedback:'आपकी प्रतिक्रिया', labelSpeed:'समाधान की गति', labelQuality:'समाधान की गुणवत्ता', labelComm:'संचार', labelComments:'टिप्पणियां', commentsOptional:'(वैकल्पिक)', commentsPh:'अतिरिक्त प्रतिक्रिया...', btnSubmit:'रेटिंग सबमिट करें →', btnSubmitting:'सबमिट हो रहा है...', btnBack:'होम पर वापस', errNoId:'कोई शिकायत ID नहीं दी गई।', errNotResolved:'यह शिकायत अभी हल नहीं हुई है।', errNotFound:'शिकायत नहीं मिली।', errLoad:'शिकायत लोड नहीं हो सकी।', errRateAll:'कृपया सभी तीन श्रेणियों को रेट करें।', alreadyRatedTitle:'पहले से रेट किया गया', alreadyRatedMsg:'आपने इस शिकायत के लिए पहले ही रेटिंग सबमिट कर दी है। धन्यवाद!', successTitle:'धन्यवाद!', successMsg:'आपकी प्रतिक्रिया दर्ज कर ली गई है।' },
      track:{ title:'🔍 अपनी शिकायत ट्रैक करें', sub:'रीयल-टाइम स्थिति देखने के लिए शिकायत ID दर्ज करें।', ph:'शिकायत ID दर्ज करें — जैसे 42', btn:'ट्रैक करें', e1:'कृपया शिकायत ID दर्ज करें', e2:'उस ID से कोई शिकायत नहीं मिली। जांचें और पुनः प्रयास करें।', e3:'शिकायत लोड नहीं हो सकी। पुनः प्रयास करें।', stage1:'सबमिट किया गया', stage1d:'शिकायत प्राप्त हुई', stage2:'AI विश्लेषण', stage2d:'कीवर्ड, जोखिम मूल्यांकन', stage3:'विभाग को सौंपा गया', stage3d:'भेजा गया: ', stage4:'प्रगति में', stage4d:'विभाग काम कर रहा है', stage5:'हल हुआ', stage5d:'समस्या हल हो गई', stage6:'रेट किया गया', stage6d:'नागरिक प्रतिक्रिया सबमिट', progressTitle:'शिकायत प्रगति', lblDept:'विभाग', lblSla:'SLA समय सीमा', resolutionStatus:'समाधान स्थिति', complaintResolved:'शिकायत हल हुई', resolvedOn:'हल हुई तारीख', pendingAssignment:'असाइनमेंट लंबित', btnRate:'⭐ समाधान रेट करें', resolutionNote:'✅ समाधान नोट:', locationTitle:'📍 शिकायत स्थान', overdueBy:'देरी', timeRemaining:'शेष समय', dueWas:'समय सीमा थी', slaBreach:'SLA उल्लंघन', dueLabel:'देय', slaUsed:'SLA उपयोग' },
      register:{ title:'खाता बनाएं', sub:'CivicCMS से जुड़ें — समस्याएं रिपोर्ट करें, प्रगति ट्रैक करें', lblName:'पूरा नाम', lblEmail:'ईमेल पता', lblPass:'पासवर्ड', passHint:'(न्यूनतम 6 अक्षर)', namePh:'आपका पूरा नाम', emailPh:'आपका@ईमेल.com', passPh:'••••••••', btnCreate:'खाता बनाएं →', btnCreating:'खाता बनाया जा रहा है...', hasAccount:'पहले से खाता है?', signIn:'यहाँ साइन इन करें →', e1:'सभी फ़ील्ड आवश्यक हैं।', e2:'पासवर्ड कम से कम 6 अक्षर होना चाहिए।', e3:'पंजीकरण विफल। पुनः प्रयास करें।', success:'खाता बन गया! जारी रखने के लिए साइन इन करें…' },
      login:{ sub:'नागरिक पोर्टल — साइन इन करें या खाता बनाएं', tabUser:'यूज़र लॉगिन', tabAdmin:'एडमिन लॉगिन', lblEmail:'ईमेल पता', lblPass:'पासवर्ड', forgotPass:'पासवर्ड भूल गए?', btnSignIn:'साइन इन करें →', orEmail:'या ईमेल से साइन इन करें', btnGoogle:'Google से जारी रखें', btnPhone:'फ़ोन नंबर से जारी रखें', noAccount:'खाता नहीं है?', createAccount:'यहाँ बनाएं →', tabL:'लॉगिन', tabR:'रजिस्टर', lblName:'पूरा नाम', passHint:'(न्यूनतम 6 अक्षर)', emailPh:'आपका@ईमेल.com', passPh:'••••••••', namePh:'आपका पूरा नाम', btnL:'साइन इन करें →', btnR:'खाता बनाएं →', e1:'ईमेल और पासवर्ड आवश्यक है।', e2:'सभी फ़ील्ड आवश्यक हैं।', e3:'पासवर्ड कम से कम 6 अक्षर होना चाहिए।', e4:'गलत क्रेडेंशियल।', e5:'पंजीकरण विफल।', fpTitle:'पासवर्ड रीसेट', fpStep1Sub:'अपना पंजीकृत ईमेल पता दर्ज करें।', fpEmailLbl:'ईमेल पता', fpContinue:'जारी रखें →', fpStep2Sub:'के लिए नया पासवर्ड चुनें', fpNewPass:'नया पासवर्ड', fpConfPass:'पासवर्ड की पुष्टि करें', fpReset:'पासवर्ड रीसेट करें →', fpSuccessTitle:'पासवर्ड रीसेट हो गया!', fpSuccessSub:'आपका पासवर्ड अपडेट हो गया। अब साइन इन करें।', fpGoLogin:'साइन इन पर जाएं →', fpBack:'← वापस', fpCancel:'रद्द करें', phoneStep1:'OTP प्राप्त करने के लिए अपना 10-अंकीय मोबाइल नंबर दर्ज करें।', phonePh:'10-अंकीय नंबर', btnSendOtp:'OTP भेजें →', btnVerifyOtp:'सत्यापित करें & साइन इन करें →' }
    }
  };

  function get() { return localStorage.getItem('civicLang') || 'en'; }
  function set(code) { localStorage.setItem('civicLang', code); }
  function current() { return LANGS[get()]; }

  function applySettings() {
    const cur = get();
    const L = current();
    const gear = document.getElementById('civic-gear-btn');
    if (gear) gear.title = L.nav.settings;
    const lbl = document.getElementById('settings-lang-label');
    if (lbl) lbl.textContent = '🌐 ' + L.nav.language;
    const loText = document.getElementById('settings-logout-text');
    if (loText) loText.textContent = L.nav.logout;
    ['en','ta','hi'].forEach(code => {
      const btn = document.getElementById('langopt-' + code);
      const chk = document.getElementById('lcheck-' + code);
      if (btn) btn.classList.toggle('active', code === cur);
      if (chk) chk.textContent = code === cur ? '✓' : '';
    });
  }

  function applyNav() {
    const nav = current().nav;
    const ids = {
      'nav-home': nav.home, 'nav-submit': nav.submit,
      'nav-track': nav.track, 'nav-my': nav.myComplaints,
      'nav-admin': nav.admin, 'nav-chatbot': nav.chatbot || '🤖 AI Chatbot'
    };
    Object.entries(ids).forEach(([id, txt]) => {
      const el = document.getElementById(id);
      if (el) el.textContent = txt;
    });
    applySettings();
  }

  // injectSwitcher is kept for backward compatibility but does nothing now
  function injectSwitcher() { applySettings(); }

  function applyLogin() {
    const L = current().login;
    const s = (id, txt) => { const el = document.getElementById(id); if (el && txt) el.textContent = txt; };
    const p = (id, txt) => { const el = document.getElementById(id); if (el && txt) el.placeholder = txt; };

    // ── Page subtitle & tabs ──────────────────────────────────────────
    s('login-subtitle', L.sub);
    s('tab-user-text',  L.tabUser);
    s('tab-admin-text', L.tabAdmin);

    // ── Email / password form ─────────────────────────────────────────
    s('lbl-email',       L.lblEmail);
    s('lbl-pass',        L.lblPass);
    p('u-email',         L.emailPh);
    p('u-pass',          L.passPh);
    s('forgot-link-text',L.forgotPass);
    s('btn-signin-text', L.btnSignIn);

    // ── Divider & social buttons ──────────────────────────────────────
    s('or-divider-text', L.orEmail);
    s('btn-google-text', L.btnGoogle);
    s('btn-phone-text',  L.btnPhone);

    // ── Register footer link ──────────────────────────────────────────
    s('no-account-text',    L.noAccount);
    s('create-account-link',L.createAccount);

    // ── Phone OTP modal ───────────────────────────────────────────────
    s('phone-modal-title', L.btnPhone);
    s('phone-step1-desc',  L.phoneStep1 || 'Enter your 10-digit mobile number to receive an OTP.');
    p('p-phone',           L.phonePh    || '10-digit number');
    s('btn-send-otp-text', L.btnSendOtp || 'Send OTP →');
    s('btn-phone-cancel',  L.fpCancel   || 'Cancel');
    s('btn-otp-back',      L.fpBack     || '← Back');
    s('btn-verify-otp-text', L.btnVerifyOtp || 'Verify & Sign In →');

    // ── Forgot password modal ─────────────────────────────────────────
    const fpH3 = document.querySelector('#fp-step1 h3');
    if (fpH3) fpH3.textContent = '🔑 ' + L.fpTitle;
    const fpSub1 = document.querySelector('#fp-step1 p');
    if (fpSub1) fpSub1.textContent = L.fpStep1Sub;
    const fpEmailLbl = document.querySelector('label[for="fp-email"]');
    if (fpEmailLbl) fpEmailLbl.textContent = L.fpEmailLbl;
    p('fp-email', L.emailPh);
    const fpBtn1 = document.getElementById('fp-btn1');
    if (fpBtn1) fpBtn1.textContent = L.fpContinue;
    const fpNewLbl = document.querySelector('label[for="fp-newpass"]');
    if (fpNewLbl) fpNewLbl.textContent = L.fpNewPass;
    const fpConfLbl = document.querySelector('label[for="fp-confirmpass"]');
    if (fpConfLbl) fpConfLbl.textContent = L.fpConfPass;
    const fpBtn2 = document.getElementById('fp-btn2');
    if (fpBtn2) fpBtn2.textContent = L.fpReset;
    const fpH3s3 = document.querySelector('#fp-step3 h3');
    if (fpH3s3) fpH3s3.textContent = L.fpSuccessTitle;
    const fpSuccP = document.querySelector('#fp-step3 p');
    if (fpSuccP) fpSuccP.textContent = L.fpSuccessSub;
    const fpGoBtn = document.querySelector('#fp-step3 button');
    if (fpGoBtn) fpGoBtn.textContent = L.fpGoLogin;
    document.querySelectorAll('.modal-cancel').forEach(btn => {
      if (btn.closest('#forgot-modal')) btn.textContent = L.fpCancel;
    });

    applySettings();
  }

  function switchTo(code) {
    set(code);
    const panel = document.getElementById('civic-settings-panel');
    if (panel) panel.classList.remove('open');

    // ── FIX: Do NOT reload the page — that can break auth-protected pages
    // by triggering a 401 → redirect to /login.html mid-session.
    //
    // Instead, call the page-level applyLang() if it exists (every page
    // that uses LangManager defines one).  Fall back to applyNav() only
    // (nav + settings panel) if the page hasn't registered a full handler.
    //
    // Auth session (localStorage token/name/email/role) is NEVER touched here.
    if (typeof window.applyLang === 'function') {
      window.applyLang();          // full in-place text swap — no navigation
    } else if (typeof applyLang === 'function') {
      applyLang();                 // same, but declared in local scope
    } else {
      applyNav();                  // minimal fallback: nav + settings only
      applySettings();
    }
  }

  function doLogout() {
    ['token','name','email','role'].forEach(k => localStorage.removeItem(k));
    window.location.href = '/login.html';
  }

  function toggleSettings(e) {
    e.stopPropagation();
    const panel = document.getElementById('civic-settings-panel');
    if (panel) panel.classList.toggle('open');
  }

  document.addEventListener('click', () => {
    const panel = document.getElementById('civic-settings-panel');
    if (panel) panel.classList.remove('open');
  });

  return { get, set, current, applyNav, applySettings, applyLogin, switchTo, doLogout, toggleSettings, injectSwitcher, LANGS };
})();

/* ══════════════════════════════════════════════════════════════
   CivicTheme — Global Dark / Light Theme Manager
   Usage: CivicTheme.set('dark'|'light') | CivicTheme.toggle()
   Persists in localStorage as 'civicTheme'
   ══════════════════════════════════════════════════════════════ */
const CivicTheme = (() => {
  const KEY = 'civicTheme';

  function get() { return localStorage.getItem(KEY) || 'light'; }

  function apply(theme) {
    document.documentElement.setAttribute('data-theme', theme);

    // Toggle icon on toggle button
    const btn = document.getElementById('theme-toggle-btn');
    if (btn) btn.textContent = theme === 'dark' ? '☀️' : '🌙';

    // Update checkmarks in settings panel
    const lChk = document.getElementById('tchk-light');
    const dChk = document.getElementById('tchk-dark');
    const lOpt = document.getElementById('theme-opt-light');
    const dOpt = document.getElementById('theme-opt-dark');
    if (lChk) lChk.textContent = theme === 'light' ? '✓' : '';
    if (dChk) dChk.textContent = theme === 'dark'  ? '✓' : '';
    if (lOpt) lOpt.classList.toggle('active', theme === 'light');
    if (dOpt) dOpt.classList.toggle('active', theme === 'dark');
  }

  function set(theme) {
    localStorage.setItem(KEY, theme);
    apply(theme);
  }

  function toggle() {
    set(get() === 'dark' ? 'light' : 'dark');
  }

  // Apply immediately on load (before DOMContentLoaded to avoid flash)
  apply(get());

  document.addEventListener('DOMContentLoaded', () => apply(get()));

  return { get, set, toggle, apply };
})();
