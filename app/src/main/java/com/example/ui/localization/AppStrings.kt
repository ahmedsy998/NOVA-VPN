package com.example.ui.localization

import androidx.compose.runtime.staticCompositionLocalOf
import com.example.data.model.AppLanguage
import com.example.data.model.ConnectionQuality
import com.example.data.model.ConnectionResult
import com.example.data.model.GeographicalRegion
import com.example.data.model.ServerStatus

data class AppStrings(
  val isArabic: Boolean,
  val appName: String,
  // Navigation
  val navHome: String,
  val navLocations: String,
  val navSpeedTest: String,
  val navHistory: String,
  val navSettings: String,
  // Home
  val statusDisconnected: String,
  val statusPreparingPermission: String,
  val statusProvisioning: String,
  val statusConnecting: String,
  val statusVerifying: String,
  val statusConnected: String,
  val statusReconnecting: String,
  val statusDisconnecting: String,
  val statusError: String,
  val tapToConnect: String,
  val tapToDisconnect: String,
  val smartLocation: String,
  val countrySelection: String,
  val selectCountryPrompt: String,
  val vpnIpLabel: String,
  val notVerifiedYet: String,
  val durationLabel: String,
  val downloadLabel: String,
  val uploadLabel: String,
  val latencyLabel: String,
  val serverLoadLabel: String,
  val qualityLabel: String,
  val privacyActiveTitle: String,
  val dnsProtectedBadge: String,
  val ipv6ProtectedBadge: String,
  val killSwitchBadge: String,
  val noLogsBadge: String,
  val devModeTitle: String,
  val devModeDescription: String,
  val configureBackendCta: String,
  val dismiss: String,
  // Permission Dialog
  val vpnPermissionTitle: String,
  val vpnPermissionBody: String,
  val vpnPermissionAccept: String,
  val vpnPermissionCancel: String,
  // Locations
  val globalLocationsTitle: String,
  val searchPlaceholder: String,
  val refreshServers: String,
  val favoritesSection: String,
  val recentSection: String,
  val allCountriesSection: String,
  val serversAvailableFormat: String,
  val serverNotAvailable: String,
  val bestPingLabel: String,
  val noMatchingCountries: String,
  val selectCityOrServer: String,
  val smartBestInCountry: String,
  val smartBestInCountryDesc: String,
  // Server Details
  val serverDetailsTitle: String,
  val countryField: String,
  val cityField: String,
  val serverIdField: String,
  val protocolsField: String,
  val latencyField: String,
  val loadField: String,
  val availabilityField: String,
  val capacityField: String,
  val qualityField: String,
  val measurePingButton: String,
  val connectButton: String,
  val disconnectButton: String,
  val serverUnavailableHint: String,
  // Speed Test
  val speedTestTitle: String,
  val speedTestSubtitle: String,
  val startSpeedTest: String,
  val runningTest: String,
  val pingMsUnit: String,
  val jitterLabel: String,
  val mbpsUnit: String,
  val beforeVpnTitle: String,
  val afterVpnTitle: String,
  val notMeasured: String,
  val speedTestDisclaimer: String,
  // History
  val historyTitle: String,
  val clearHistory: String,
  val clearHistoryConfirmTitle: String,
  val clearHistoryConfirmBody: String,
  val confirm: String,
  val cancel: String,
  val emptyHistoryTitle: String,
  val emptyHistorySubtitle: String,
  val privacyHistoryNote: String,
  // Settings
  val settingsTitle: String,
  val sectionConnection: String,
  val vpnProtocolLabel: String,
  val smartSelectionLabel: String,
  val smartSelectionDesc: String,
  val connectionTimeoutLabel: String,
  val sectionPrivacy: String,
  val killSwitchLabel: String,
  val killSwitchDesc: String,
  val openSystemVpnSettings: String,
  val dnsProtectionLabel: String,
  val dnsProtectionDesc: String,
  val secureDnsProviderLabel: String,
  val ipv6ProtectionLabel: String,
  val ipv6ProtectionDesc: String,
  val autoConnectBootLabel: String,
  val autoConnectWifiLabel: String,
  val splitTunnelingLabel: String,
  val splitTunnelingDesc: String,
  val configureApps: String,
  val sectionBackend: String,
  val backendUrlLabel: String,
  val backendUrlPlaceholder: String,
  val saveBackendUrl: String,
  val accountSessionLabel: String,
  val signInButton: String,
  val registerButton: String,
  val signOutButton: String,
  val sectionAppearance: String,
  val languageLabel: String,
  val themeLabel: String,
  val themeDark: String,
  val themeLight: String,
  val themeSystem: String,
  val notificationsLabel: String,
  val sectionAbout: String,
  val privacyPolicyTitle: String,
  val privacyPolicyBody: String,
  val aboutVersionLabel: String,
) {
  fun regionLabel(region: GeographicalRegion): String = if (isArabic) {
    when (region) {
      GeographicalRegion.ALL -> "الكل"
      GeographicalRegion.MENA -> "الشرق الأوسط وشمال أفريقيا"
      GeographicalRegion.EUROPE -> "أوروبا"
      GeographicalRegion.NORTH_AMERICA -> "أمريكا الشمالية"
      GeographicalRegion.SOUTH_AMERICA -> "أمريكا الجنوبية"
      GeographicalRegion.ASIA_PACIFIC -> "آسيا والمحيط الهادئ"
      GeographicalRegion.SUB_SAHARAN_AFRICA -> "أفريقيا جنوب الصحراء"
      GeographicalRegion.CENTRAL_ASIA -> "آسيا الوسطى"
      GeographicalRegion.CARIBBEAN_OCEANIA -> "الكاريبي وأوقيانوسيا"
    }
  } else {
    when (region) {
      GeographicalRegion.ALL -> "All"
      GeographicalRegion.MENA -> "Middle East & N. Africa"
      GeographicalRegion.EUROPE -> "Europe"
      GeographicalRegion.NORTH_AMERICA -> "North America"
      GeographicalRegion.SOUTH_AMERICA -> "South America"
      GeographicalRegion.ASIA_PACIFIC -> "Asia-Pacific"
      GeographicalRegion.SUB_SAHARAN_AFRICA -> "Sub-Saharan Africa"
      GeographicalRegion.CENTRAL_ASIA -> "Central Asia"
      GeographicalRegion.CARIBBEAN_OCEANIA -> "Caribbean & Oceania"
    }
  }

  fun serverStatusLabel(status: ServerStatus): String = if (isArabic) {
    when (status) {
      ServerStatus.ONLINE -> "متصل"
      ServerStatus.BUSY -> "مزدحم"
      ServerStatus.MAINTENANCE -> "صيانة"
      ServerStatus.OFFLINE -> "غير متصل"
    }
  } else {
    when (status) {
      ServerStatus.ONLINE -> "Online"
      ServerStatus.BUSY -> "Busy"
      ServerStatus.MAINTENANCE -> "Maintenance"
      ServerStatus.OFFLINE -> "Offline"
    }
  }

  fun qualityLabelText(quality: ConnectionQuality): String = if (isArabic) {
    when (quality) {
      ConnectionQuality.EXCELLENT -> "ممتازة"
      ConnectionQuality.GOOD -> "جيدة"
      ConnectionQuality.FAIR -> "متوسطة"
      ConnectionQuality.POOR -> "ضعيفة"
      ConnectionQuality.UNKNOWN -> "غير مقاسة"
    }
  } else {
    when (quality) {
      ConnectionQuality.EXCELLENT -> "Excellent"
      ConnectionQuality.GOOD -> "Good"
      ConnectionQuality.FAIR -> "Fair"
      ConnectionQuality.POOR -> "Poor"
      ConnectionQuality.UNKNOWN -> "Not measured"
    }
  }

  fun historyResultLabel(result: ConnectionResult): String = if (isArabic) {
    when (result) {
      ConnectionResult.CONNECTED -> "تم الاتصال"
      ConnectionResult.DISCONNECTED -> "قطع طبيعي"
      ConnectionResult.FAILED -> "فشل الاتصال"
      ConnectionResult.PERMISSION_REVOKED -> "تم سحب الإذن"
    }
  } else {
    when (result) {
      ConnectionResult.CONNECTED -> "Connected"
      ConnectionResult.DISCONNECTED -> "Disconnected"
      ConnectionResult.FAILED -> "Failed"
      ConnectionResult.PERMISSION_REVOKED -> "Permission Revoked"
    }
  }
}

val ArabicStrings = AppStrings(
  isArabic = true,
  appName = "NOVA VPN",
  navHome = "الرئيسية",
  navLocations = "المواقع",
  navSpeedTest = "السرعة",
  navHistory = "السجل",
  navSettings = "الإعدادات",
  statusDisconnected = "غير متصل",
  statusPreparingPermission = "بانتظار إذن النظام",
  statusProvisioning = "جارٍ تأمين المفاتيح…",
  statusConnecting = "جارٍ إنشاء النفق…",
  statusVerifying = "جارٍ التحقق من النفق…",
  statusConnected = "محمي ومتصل",
  statusReconnecting = "جارٍ إعادة الاتصال…",
  statusDisconnecting = "جارٍ قطع الاتصال…",
  statusError = "تعذّر الاتصال",
  tapToConnect = "اضغط للاتصال",
  tapToDisconnect = "اضغط للإيقاف",
  smartLocation = "الموقع الذكي",
  countrySelection = "اختيار الدولة",
  selectCountryPrompt = "اختر دولة متاح بها خادم",
  vpnIpLabel = "عنوان VPN المتحقق منه",
  notVerifiedYet = "غير متحقق",
  durationLabel = "مدة الاتصال",
  downloadLabel = "التنزيل",
  uploadLabel = "الرفع",
  latencyLabel = "زمن الاستجابة",
  serverLoadLabel = "حمل الخادم",
  qualityLabel = "جودة الاتصال",
  privacyActiveTitle = "طبقات الحماية النشطة",
  dnsProtectedBadge = "حماية DNS",
  ipv6ProtectedBadge = "حماية IPv6",
  killSwitchBadge = "مفتاح الإيقاف",
  noLogsBadge = "دون سجلات",
  devModeTitle = "وضع التطوير / الخادم غير مُعد",
  devModeDescription = "لا توجد خوادم وهمية مضمنة. لإنشاء أنفاق WireGuard حقيقية، أضف رابط خادم NOVA VPN المنشور في الإعدادات.",
  configureBackendCta = "إعداد الخادم",
  dismiss = "إخفاء",
  vpnPermissionTitle = "تأكيد إذن نفق VPN",
  vpnPermissionBody = "يحتاج NOVA VPN إلى إذن أندرويد لإنشاء واجهة شبكة مشفّرة (VpnService) وتوجيه اتصالاتك عبر بروتوكول WireGuard مع حماية تسرب DNS وIPv6.",
  vpnPermissionAccept = "متابعة ومنح الإذن",
  vpnPermissionCancel = "إلغاء",
  globalLocationsTitle = "الشبكة العالمية (ISO 3166-1)",
  searchPlaceholder = "ابحث بالدولة أو المدينة أو المنطقة…",
  refreshServers = "تحديث الخوادم",
  favoritesSection = "المفضلة",
  recentSection = "المستخدمة مؤخراً",
  allCountriesSection = "جميع الدول والأقاليم",
  serversAvailableFormat = "خوادم متاحة",
  serverNotAvailable = "لا يوجد خادم متاح حالياً",
  bestPingLabel = "أفضل استجابة",
  noMatchingCountries = "لا توجد نتائج مطابقة لبحثك",
  selectCityOrServer = "اختر المدينة أو الخادم",
  smartBestInCountry = "الاختيار الذكي داخل الدولة",
  smartBestInCountryDesc = "يختار تلقائياً أسرع وأقل خادم حملاً داخل نفس الدولة",
  serverDetailsTitle = "تفاصيل الخادم",
  countryField = "الدولة",
  cityField = "المدينة",
  serverIdField = "معرّف الخادم",
  protocolsField = "البروتوكولات المدعومة",
  latencyField = "زمن الاستجابة المباشر",
  loadField = "نسبة الحمل",
  availabilityField = "حالة التوفر",
  capacityField = "السعة المتاحة",
  qualityField = "جودة الاتصال التقديرية",
  measurePingButton = "قياس الاستجابة الآن",
  connectButton = "اتصال بالخادم",
  disconnectButton = "قطع الاتصال",
  serverUnavailableHint = "هذا الخادم غير متصل حالياً أو قيد الصيانة ولا يمكن الاتصال به.",
  speedTestTitle = "اختبار سرعة الشبكة",
  speedTestSubtitle = "قياسات حقيقية لزمن الاستجابة وسرعة التنزيل والرفع دون أي تقديرات وهمية.",
  startSpeedTest = "بدء الاختبار الحقيقي",
  runningTest = "جارٍ القياس…",
  pingMsUnit = "مللي ثانية",
  jitterLabel = "تذبذب الإشارة",
  mbpsUnit = "Mbps",
  beforeVpnTitle = "قبل الـ VPN",
  afterVpnTitle = "عبر نفق VPN",
  notMeasured = "غير مقاس",
  speedTestDisclaimer = "ملاحظة تقنية: يحافظ الـ VPN على أعلى قدر ممكن من سرعة اتصالك الأصلية دون تجاوز السعة الفيزيائية لخط الإنترنت لديك.",
  historyTitle = "سجل الاتصال",
  clearHistory = "مسح السجل",
  clearHistoryConfirmTitle = "مسح سجل الاتصال؟",
  clearHistoryConfirmBody = "سيتم حذف سجل الجلسات المخزن محلياً على جهازك فقط.",
  confirm = "تأكيد",
  cancel = "إلغاء",
  emptyHistoryTitle = "لا توجد جلسات اتصال مسجلة",
  emptyHistorySubtitle = "يتم تسجيل التاريخ والدولة والمدة والنتيجة محلياً فقط دون أي مراقبة لمحتوى التصفح.",
  privacyHistoryNote = "الخصوصية أولاً: لا نجمع أو نخزن سجل المواقع أو محتوى حركة المرور.",
  settingsTitle = "الإعدادات والأمان",
  sectionConnection = "بروتوكول الاتصال والذكاء",
  vpnProtocolLabel = "بروتوكول VPN",
  smartSelectionLabel = "محرك الاختيار الذكي للخوادم",
  smartSelectionDesc = "اختيار تلقائي بناءً على الاستجابة والحمل والاستقرار",
  connectionTimeoutLabel = "مهلة الاتصال (بالثواني)",
  sectionPrivacy = "الخصوصية المتقدمة والحماية",
  killSwitchLabel = "مفتاح الإيقاف التلقائي (Kill Switch)",
  killSwitchDesc = "منع تسرب الاتصال خارج النفق عند تفعيل إعدادات النظام",
  openSystemVpnSettings = "إعدادات قفل VPN في النظام",
  dnsProtectionLabel = "حماية تسرب DNS",
  dnsProtectionDesc = "توجيه استعلامات DNS داخل النفق عبر خوادم مشفّرة",
  secureDnsProviderLabel = "مزود DNS الآمن",
  ipv6ProtectionLabel = "حماية تسرب IPv6",
  ipv6ProtectionDesc = "منع تجاوز النفق عبر مسارات IPv6 المكشوفة",
  autoConnectBootLabel = "اتصال تلقائي عند تشغيل الجهاز",
  autoConnectWifiLabel = "اتصال تلقائي على شبكات Wi-Fi",
  splitTunnelingLabel = "تقسيم النفق للتطبيقات (Split Tunneling)",
  splitTunnelingDesc = "تحديد التطبيقات المستثناة أو المشمولة بالنفق",
  configureApps = "تحديد التطبيقات",
  sectionBackend = "البنية التحتية للخادم والمصادقة",
  backendUrlLabel = "رابط خادم NOVA VPN (HTTPS)",
  backendUrlPlaceholder = "https://vpn-api.yourdomain.com",
  saveBackendUrl = "حفظ الرابط ومزامنة المواقع",
  accountSessionLabel = "جلسة المستخدم",
  signInButton = "تسجيل الدخول",
  registerButton = "إنشاء حساب",
  signOutButton = "تسجيل الخروج",
  sectionAppearance = "اللغة والمظهر",
  languageLabel = "لغة التطبيق واتجاه الواجهة",
  themeLabel = "سمة المظهر",
  themeDark = "داكن كحلي",
  themeLight = "فاتح",
  themeSystem = "النظام",
  notificationsLabel = "إشعارات حالة الاتصال",
  sectionAbout = "سياسة الخصوصية وحول التطبيق",
  privacyPolicyTitle = "تعهد عدم الاحتفاظ بالسجلات",
  privacyPolicyBody = "تُنشأ مفاتيح WireGuard الخاصة على جهازك وتُحفظ مشفّرة عبر Android Keystore. لا يجمع التطبيق أو يفحص سجل تصفحك أو المواقع التي تزورها.",
  aboutVersionLabel = "إصدار التطبيق",
)

val EnglishStrings = AppStrings(
  isArabic = false,
  appName = "NOVA VPN",
  navHome = "Home",
  navLocations = "Locations",
  navSpeedTest = "Speed",
  navHistory = "History",
  navSettings = "Settings",
  statusDisconnected = "Disconnected",
  statusPreparingPermission = "Awaiting VPN Permission",
  statusProvisioning = "Provisioning Keys…",
  statusConnecting = "Establishing Tunnel…",
  statusVerifying = "Verifying Tunnel…",
  statusConnected = "Protected & Connected",
  statusReconnecting = "Reconnecting…",
  statusDisconnecting = "Disconnecting…",
  statusError = "Connection Error",
  tapToConnect = "Tap to Connect",
  tapToDisconnect = "Tap to Disconnect",
  smartLocation = "Smart Location",
  countrySelection = "Select Country",
  selectCountryPrompt = "Choose a country with deployed servers",
  vpnIpLabel = "Verified VPN IP",
  notVerifiedYet = "Not verified",
  durationLabel = "Duration",
  downloadLabel = "Download",
  uploadLabel = "Upload",
  latencyLabel = "Latency",
  serverLoadLabel = "Server Load",
  qualityLabel = "Quality",
  privacyActiveTitle = "Active Privacy Shields",
  dnsProtectedBadge = "DNS Shield",
  ipv6ProtectedBadge = "IPv6 Guard",
  killSwitchBadge = "Kill Switch",
  noLogsBadge = "Zero Logs",
  devModeTitle = "Development Mode / Backend Unconfigured",
  devModeDescription = "No fake VPN servers are bundled. Configure your deployed NOVA VPN backend HTTPS URL in Settings to provision genuine WireGuard tunnels.",
  configureBackendCta = "Configure Backend",
  dismiss = "Dismiss",
  vpnPermissionTitle = "VPN Tunnel Permission",
  vpnPermissionBody = "NOVA VPN requires Android VpnService permission to create an encrypted WireGuard network interface and enforce DNS/IPv6 leak protection.",
  vpnPermissionAccept = "Continue & Grant",
  vpnPermissionCancel = "Cancel",
  globalLocationsTitle = "Global Network (ISO 3166-1)",
  searchPlaceholder = "Search country, city, or region…",
  refreshServers = "Sync Servers",
  favoritesSection = "Favorites",
  recentSection = "Recently Connected",
  allCountriesSection = "All Countries & Territories",
  serversAvailableFormat = "servers online",
  serverNotAvailable = "Server not currently available",
  bestPingLabel = "Best Ping",
  noMatchingCountries = "No countries match your search",
  selectCityOrServer = "Select City or Server",
  smartBestInCountry = "Smart Selection in Country",
  smartBestInCountryDesc = "Automatically chooses the lowest-latency, lowest-load node inside this country",
  serverDetailsTitle = "Server Details",
  countryField = "Country",
  cityField = "City",
  serverIdField = "Server Identifier",
  protocolsField = "Supported Protocols",
  latencyField = "Live Latency",
  loadField = "Server Load",
  availabilityField = "Availability Status",
  capacityField = "Available Capacity",
  qualityField = "Estimated Quality",
  measurePingButton = "Measure Latency Now",
  connectButton = "Connect to Server",
  disconnectButton = "Disconnect",
  serverUnavailableHint = "This server is offline or in maintenance and cannot accept tunnel connections.",
  speedTestTitle = "Network Speed Test",
  speedTestSubtitle = "Real HTTP ping, jitter, download, and upload measurements with zero fabricated metrics.",
  startSpeedTest = "Start Real Speed Test",
  runningTest = "Measuring…",
  pingMsUnit = "ms",
  jitterLabel = "Jitter",
  mbpsUnit = "Mbps",
  beforeVpnTitle = "Before VPN",
  afterVpnTitle = "Over VPN",
  notMeasured = "Not measured",
  speedTestDisclaimer = "Engineering note: A VPN preserves as much of your physical ISP bandwidth as possible—it cannot exceed your underlying line capacity.",
  historyTitle = "Connection History",
  clearHistory = "Clear History",
  clearHistoryConfirmTitle = "Clear connection history?",
  clearHistoryConfirmBody = "This deletes locally stored session logs from your device.",
  confirm = "Confirm",
  cancel = "Cancel",
  emptyHistoryTitle = "No Connection Sessions Recorded",
  emptyHistorySubtitle = "Only date, country, duration, and status are stored locally—never browsing activity.",
  privacyHistoryNote = "Zero-browsing-logs policy: visited sites and packet contents are never inspected or stored.",
  settingsTitle = "Settings & Security",
  sectionConnection = "VPN Protocol & Intelligence",
  vpnProtocolLabel = "VPN Protocol",
  smartSelectionLabel = "Smart Server Selection Engine",
  smartSelectionDesc = "Automatically scores latency, load, capacity, and stability",
  connectionTimeoutLabel = "Connection Timeout (seconds)",
  sectionPrivacy = "Advanced Privacy & Protection",
  killSwitchLabel = "Kill Switch Guard",
  killSwitchDesc = "Blocks non-tunneled traffic when Android VPN Lockdown is active",
  openSystemVpnSettings = "Android System VPN Lockdown",
  dnsProtectionLabel = "DNS Leak Protection",
  dnsProtectionDesc = "Routes all DNS queries through encrypted tunnel DNS servers",
  secureDnsProviderLabel = "Secure DNS Resolver",
  ipv6ProtectionLabel = "IPv6 Leak Protection",
  ipv6ProtectionDesc = "Captures ::/0 routes inside the tunnel to prevent IPv6 leaks",
  autoConnectBootLabel = "Auto-Connect on Device Boot",
  autoConnectWifiLabel = "Auto-Connect on Wi-Fi Networks",
  splitTunnelingLabel = "App Split Tunneling",
  splitTunnelingDesc = "Exclude or include specific installed apps",
  configureApps = "Select Apps",
  sectionBackend = "Backend Infrastructure & Auth",
  backendUrlLabel = "NOVA VPN Backend URL (HTTPS)",
  backendUrlPlaceholder = "https://vpn-api.yourdomain.com",
  saveBackendUrl = "Save URL & Sync Locations",
  accountSessionLabel = "User Session",
  signInButton = "Sign In",
  registerButton = "Create Account",
  signOutButton = "Sign Out",
  sectionAppearance = "Language & Theme",
  languageLabel = "App Language & Layout Direction",
  themeLabel = "Appearance Theme",
  themeDark = "Dark Navy",
  themeLight = "Light",
  themeSystem = "System",
  notificationsLabel = "Tunnel Status Notifications",
  sectionAbout = "Privacy & About",
  privacyPolicyTitle = "Zero-Log Architecture",
  privacyPolicyBody = "WireGuard private keys are generated on-device and encrypted using Android Keystore. NOVA VPN never logs, inspects, or sells browsing history.",
  aboutVersionLabel = "App Version",
)

fun stringsForLanguage(language: AppLanguage): AppStrings =
  if (language == AppLanguage.ARABIC) ArabicStrings else EnglishStrings

val LocalAppStrings = staticCompositionLocalOf { ArabicStrings }
