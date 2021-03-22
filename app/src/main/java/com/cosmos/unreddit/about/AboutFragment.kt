package com.cosmos.unreddit.about

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.cosmos.unreddit.BuildConfig
import com.cosmos.unreddit.R
import com.cosmos.unreddit.base.BaseFragment
import com.cosmos.unreddit.databinding.FragmentAboutBinding
import com.cosmos.unreddit.model.CreditItem
import com.cosmos.unreddit.util.openExternalLink
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

@ExperimentalStdlibApi
class AboutFragment : BaseFragment() {

    private var _binding: FragmentAboutBinding? = null
    private val binding get() = _binding!!

    private lateinit var creditAdapter: CreditAdapter

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
        lifecycleScope.launch(coroutinesContext) {
            val items = buildList {
                add(CreditItem.Section(R.string.about_section_credits))
                addAll(CREDITS.sortedBy { it.title })
                add(CreditItem.Section(R.string.about_section_libraries))
                addAll(LIBRARIES.sortedBy { it.title })
            }
            creditAdapter.submitList(items)
        }
    }

    private fun initRecyclerView() {
        creditAdapter = CreditAdapter {
            showCreditDialog(it)
        }

        binding.listAbout.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = creditAdapter
        }
    }

    private fun initAboutSection() {
        binding.appVersion.text = BuildConfig.VERSION_NAME
    }

    private fun initAppBar() {
        binding.appBar.backCard.setOnClickListener { onBackPressed() }
    }

    private fun showCreditDialog(credit: CreditItem.Credit) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(credit.title)
            .setMessage(credit.description)
            .setPositiveButton(R.string.dialog_credit_show_website) { _, _ ->
                openExternalLink(credit.link)
            }
            .setNegativeButton(R.string.dialog_credit_show_license) { _, _ ->
                openExternalLink(credit.licenseLink)
            }
            .setCancelable(true)
            .show()
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
                )
            )
        }
    }
}
