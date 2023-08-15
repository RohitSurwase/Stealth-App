package com.cosmos.unreddit.ui.about

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.cosmos.unreddit.BuildConfig
import com.cosmos.unreddit.R
import com.cosmos.unreddit.data.model.CreditItem
import com.cosmos.unreddit.databinding.FragmentAboutBinding
import com.cosmos.unreddit.ui.base.BaseFragment
import com.cosmos.unreddit.util.extension.applyWindowInsets
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

@ExperimentalStdlibApi
class AboutFragment : BaseFragment() {

    private var _binding: FragmentAboutBinding? = null
    private val binding get() = _binding!!

    private lateinit var creditAdapter: CreditAdapter

    private val gitlabLink by lazy { getString(R.string.gitlab_link) }
    private val matrixLink by lazy { getString(R.string.matrix_link) }
    private val email by lazy { getString(R.string.email) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAboutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initAppBar()
        initAboutSection()
        initRecyclerView()
        initCredits()
    }

    private fun initCredits(coroutinesContext: CoroutineContext = Dispatchers.Default) {
        lifecycleScope.launch {
            val items = withContext(coroutinesContext) {
                buildList {
                    add(CreditItem.Section(R.string.about_section_credits))
                    addAll(CREDITS.sortedBy { it.title })
                    add(CreditItem.Section(R.string.about_section_libraries))
                    addAll(LIBRARIES.sortedBy { it.title })
                    add(CreditItem.Section(R.string.about_section_contributors))
                    addAll(CONTRIBUTORS.sortedBy { it.name })
                }
            }
            creditAdapter.submitList(items)
        }
    }

    private fun initRecyclerView() {
        creditAdapter = CreditAdapter {
            if (it is CreditItem.Credit) {
                showCreditDialog(it)
            } else if (it is CreditItem.Contributor) {
                linkHandler.openBrowser(it.link)
            }
        }

        binding.listAbout.apply {
            applyWindowInsets(left = false, top = false, right = false)
            layoutManager = LinearLayoutManager(context)
            adapter = creditAdapter
        }
    }

    private fun initAboutSection() {
        binding.appVersion.text = BuildConfig.VERSION_NAME
    }

    private fun initAppBar() {
        binding.appBar.run {
            backCard.setOnClickListener { onBackPressed() }
            buttonGitlab.setOnClickListener { linkHandler.openBrowser(gitlabLink) }
            buttonMatrix.setOnClickListener { linkHandler.openBrowser(matrixLink) }
            buttonMail.setOnClickListener { sendEmail() }
        }
    }

    private fun showCreditDialog(credit: CreditItem.Credit) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(credit.title)
            .setMessage(credit.description)
            .setPositiveButton(R.string.dialog_credit_show_website) { _, _ ->
                linkHandler.openBrowser(credit.link)
            }
            .setNegativeButton(R.string.dialog_credit_show_license) { _, _ ->
                linkHandler.openBrowser(credit.licenseLink)
            }
            .setCancelable(true)
            .show()
    }

    private fun sendEmail() {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
        }

        val packageManager = activity?.packageManager ?: return

        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            val clipboard =
                activity?.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?

            if (clipboard != null) {
                val clip = ClipData.newPlainText("CosmosDev email", email)
                clipboard.setPrimaryClip(clip)
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
                    Toast.makeText(
                        requireContext(),
                        R.string.toast_clipboard_copied_email,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private val CREDITS: List<CreditItem.Credit> by lazy {
            listOf(
                CreditItem.Credit(
                    "Feather",
                    "Cole Bemis",
                    "Simply beautiful open source icons",
                    "https://feathericons.com/",
                    CreditItem.Credit.LicenseType.MIT,
                    "https://github.com/feathericons/feather/blob/master/LICENSE"
                ),
                CreditItem.Credit(
                    "Mono Icons",
                    "Mono Company BV",
                    "The Mono icon font is a simple, consistent open-source icon set designed to be used in a wide variety of digital products.",
                    "http://icons.mono.company/",
                    CreditItem.Credit.LicenseType.MIT,
                    "https://github.com/mono-company/mono-icons/blob/master/LICENSE.md"
                ),
                CreditItem.Credit(
                    "Heroicons",
                    "Refactoring UI Inc.",
                    "A set of free MIT-licensed high-quality SVG icons for UI development.",
                    "https://heroicons.com/",
                    CreditItem.Credit.LicenseType.MIT,
                    "https://github.com/tailwindlabs/heroicons/blob/master/LICENSE"
                ),
                CreditItem.Credit(
                    "Remix Icon",
                    "Remix Design",
                    "Open source neutral style icon system",
                    "https://remixicon.com/",
                    CreditItem.Credit.LicenseType.APACHE_V2,
                    "https://github.com/Remix-Design/RemixIcon/blob/master/License"
                ),
                CreditItem.Credit(
                    "Tabler Icons",
                    "Paweł Kuna",
                    "Customizable SVG icons",
                    "https://tablericons.com/",
                    CreditItem.Credit.LicenseType.MIT,
                    "https://tablericons.com/"
                ),
                CreditItem.Credit(
                    "Material Design Icons",
                    "Google",
                    "Material Design icons by Google",
                    "https://material.io/resources/icons",
                    CreditItem.Credit.LicenseType.APACHE_V2,
                    "https://github.com/google/material-design-icons/blob/master/LICENSE"
                ),
                CreditItem.Credit(
                    "unDraw",
                    "Katerina Limpitsouni",
                    "Open-source illustrations for any idea you can imagine and create",
                    "https://undraw.co/",
                    CreditItem.Credit.LicenseType.OTHER,
                    "https://undraw.co/license"
                ),
                CreditItem.Credit(
                    "Rubik",
                    "Hubert and Fischer",
                    "Rubik is a sans serif font family with slightly rounded corners designed by Philipp Hubert and Sebastian Fischer at Hubert & Fischer as part of the Chrome Cube Lab project.",
                    "https://hubertfischer.com/work/type-rubik",
                    CreditItem.Credit.LicenseType.OPEN_FONT_V1_1,
                    "https://www.fontsquirrel.com/license/rubik"
                ),
                CreditItem.Credit(
                    "Lato",
                    "Łukasz Dziedzic",
                    "Lato is a sans serif typeface family started in the summer of 2010 by Warsaw-based designer Łukasz Dziedzic (\"Lato\" means \"Summer\" in Polish).",
                    "www.latofonts.com",
                    CreditItem.Credit.LicenseType.OPEN_FONT_V1_1,
                    "https://www.fontsquirrel.com/license/lato"
                )
            )
        }

        private val LIBRARIES: List<CreditItem.Credit> by lazy {
            listOf(
                CreditItem.Credit(
                    "Dagger Hilt",
                    "Google",
                    "Hilt provides a standard way to incorporate Dagger dependency injection into an Android application.",
                    "https://github.com/google/dagger",
                    CreditItem.Credit.LicenseType.APACHE_V2,
                    "https://github.com/google/dagger/blob/master/LICENSE.txt"
                ),
                CreditItem.Credit(
                    "Material Components for Android",
                    "Google",
                    "Modular and customizable Material Design UI components for Android",
                    "https://github.com/material-components/material-components-android",
                    CreditItem.Credit.LicenseType.APACHE_V2,
                    "https://github.com/material-components/material-components-android/blob/master/LICENSE"
                ),
                CreditItem.Credit(
                    "Retrofit",
                    "Square",
                    "A type-safe HTTP client for Android and the JVM",
                    "https://github.com/square/retrofit",
                    CreditItem.Credit.LicenseType.APACHE_V2,
                    "https://github.com/square/retrofit/blob/master/LICENSE.txt"
                ),
                CreditItem.Credit(
                    "Moshi",
                    "Square",
                    "A modern JSON library for Kotlin and Java.",
                    "https://github.com/square/moshi",
                    CreditItem.Credit.LicenseType.APACHE_V2,
                    "https://github.com/square/moshi/blob/master/LICENSE.txt"
                ),
                CreditItem.Credit(
                    "Coil",
                    "Coil Contributors",
                    "Image loading for Android backed by Kotlin Coroutines.",
                    "https://github.com/coil-kt/coil",
                    CreditItem.Credit.LicenseType.APACHE_V2,
                    "https://github.com/coil-kt/coil/blob/master/LICENSE.txt"
                ),
                CreditItem.Credit(
                    "TouchImageView for Android",
                    "Michael Ortiz",
                    "Adds touch functionality to Android ImageView.",
                    "https://github.com/MikeOrtiz/TouchImageView",
                    CreditItem.Credit.LicenseType.MIT,
                    "https://github.com/MikeOrtiz/TouchImageView/blob/master/LICENSE"
                ),
                CreditItem.Credit(
                    "ExoPlayer",
                    "Google",
                    "An extensible media player for Android",
                    "https://github.com/google/ExoPlayer",
                    CreditItem.Credit.LicenseType.APACHE_V2,
                    "https://github.com/google/ExoPlayer/blob/release-v2/LICENSE"
                ),
                CreditItem.Credit(
                    "jsoup",
                    "Jonathan Hedley",
                    "jsoup: the Java HTML parser, built for HTML editing, cleaning, scraping, and XSS safety.",
                    "https://github.com/jhy/jsoup",
                    CreditItem.Credit.LicenseType.MIT,
                    "https://github.com/jhy/jsoup/blob/master/LICENSE"
                ),
                CreditItem.Credit(
                    "FullDraggableDrawer",
                    "Drakeet Xu",
                    "Make Android DrawerLayout can be dragged out in real-time within the range of fullscreen",
                    "https://github.com/PureWriter/FullDraggableDrawer",
                    CreditItem.Credit.LicenseType.APACHE_V2,
                    "https://github.com/PureWriter/FullDraggableDrawer/blob/master/LICENSE"
                ),
                CreditItem.Credit(
                    "SSPullToRefresh",
                    "Simform Solutions",
                    "Pull to Refresh with custom animations.",
                    "https://github.com/SimformSolutionsPvtLtd/SSPullToRefresh",
                    CreditItem.Credit.LicenseType.MIT,
                    "https://github.com/SimformSolutionsPvtLtd/SSPullToRefresh/blob/main/LICENSE"
                )
            )
        }

        private val CONTRIBUTORS: List<CreditItem.Contributor> by lazy {
            listOf(
                CreditItem.Contributor(
                    "uDEV2019",
                    "@uDEV2019",
                    R.string.contributor_uDEV2019_description,
                    "https://gitlab.com/uDEV2019"
                ),
                CreditItem.Contributor(
                    "Another Sapiens",
                    "@another-sapiens",
                    R.string.contributor_anothersapiens_description,
                    "https://gitlab.com/another-sapiens"
                ),
                CreditItem.Contributor(
                    "matt wiggins",
                    "@mwiggins",
                    R.string.contributor_mwiggins_description,
                    "https://gitlab.com/mwiggins"
                )
            )
        }
    }
}
