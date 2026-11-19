package org.fossify.clock.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.fossify.clock.BuildConfig
import org.fossify.clock.R
import org.fossify.clock.ui.components.ClockTopBar
import org.fossify.clock.ui.theme.ClockTheme
import org.fossify.commons.activities.FAQActivity
import org.fossify.commons.activities.LicenseActivity
import org.fossify.commons.extensions.launchViewIntent
import org.fossify.commons.helpers.APP_FAQ
import org.fossify.commons.helpers.APP_ICON_IDS
import org.fossify.commons.helpers.APP_LAUNCHER_NAME
import org.fossify.commons.helpers.APP_LICENSES
import org.fossify.commons.helpers.LICENSE_AUTOFITTEXTVIEW
import org.fossify.commons.helpers.LICENSE_NUMBER_PICKER
import org.fossify.commons.helpers.LICENSE_RTL
import org.fossify.commons.models.FAQItem
import org.fossify.commons.R as CommonsR

class AboutActivity : SimpleActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ClockTheme(window = window) {
                Scaffold(
                    topBar = { ClockTopBar(CommonsR.string.about, ::finish) },
                ) { contentPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(contentPadding)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_launcher_foreground),
                            contentDescription = null,
                            modifier = Modifier
                                .size(72.dp)
                                .background(colorResource(R.color.launcher_background), CircleShape),
                        )
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.headlineMedium,
                            modifier = Modifier.semantics { heading() },
                        )
                        Text(
                            text = stringResource(CommonsR.string.version_placeholder, BuildConfig.VERSION_NAME),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(text = packageName, style = MaterialTheme.typography.bodySmall)
                        Text(
                            text = stringResource(R.string.about_description),
                            modifier = Modifier.padding(vertical = 8.dp),
                        )
                        TextButton(onClick = { launchViewIntent(FORK_URL) }) {
                            Text(stringResource(R.string.about_source))
                        }
                        TextButton(onClick = { launchViewIntent("$FORK_URL/releases") }) {
                            Text(stringResource(R.string.about_releases))
                        }
                        TextButton(onClick = { launchViewIntent("https://github.com/FossifyOrg/Clock") }) {
                            Text(stringResource(R.string.about_upstream))
                        }
                        TextButton(onClick = ::openFaq) {
                            Text(stringResource(CommonsR.string.frequently_asked_questions))
                        }
                        TextButton(onClick = { launchViewIntent("$FORK_URL/blob/HEAD/LICENSE") }) {
                            Text(stringResource(R.string.about_license))
                        }
                        TextButton(onClick = ::openThirdPartyLicenses) {
                            Text(stringResource(CommonsR.string.third_party_licences))
                        }
                    }
                }
            }
        }
    }

    private fun openFaq() {
        val faqItems = arrayListOf(
            FAQItem(
                title = R.string.faq_1_title,
                text = R.string.faq_1_text
            ),
            FAQItem(
                title = org.fossify.commons.R.string.faq_1_title_commons,
                text = org.fossify.commons.R.string.faq_1_text_commons
            ),
            FAQItem(
                title = org.fossify.commons.R.string.faq_4_title_commons,
                text = org.fossify.commons.R.string.faq_4_text_commons
            ),
            FAQItem(
                title = org.fossify.commons.R.string.faq_9_title_commons,
                text = org.fossify.commons.R.string.faq_9_text_commons
            )
        )

        if (resources.getBoolean(org.fossify.commons.R.bool.is_google_play_build)) {
            faqItems.add(
                FAQItem(
                    title = org.fossify.commons.R.string.faq_2_title_commons,
                    text = org.fossify.commons.R.string.faq_2_text_commons
                )
            )
            faqItems.add(
                FAQItem(
                    title = org.fossify.commons.R.string.faq_6_title_commons,
                    text = org.fossify.commons.R.string.faq_6_text_commons
                )
            )
        }

        startActivity(Intent(this, FAQActivity::class.java).apply {
            putExtra(APP_ICON_IDS, getAppIconIDs())
            putExtra(APP_LAUNCHER_NAME, getAppLauncherName())
            putExtra(APP_FAQ, faqItems)
        })
    }

    private fun openThirdPartyLicenses() {
        startActivity(Intent(this, LicenseActivity::class.java).apply {
            putExtra(APP_ICON_IDS, getAppIconIDs())
            putExtra(APP_LAUNCHER_NAME, getAppLauncherName())
            putExtra(APP_LICENSES, LICENSE_NUMBER_PICKER or LICENSE_RTL or LICENSE_AUTOFITTEXTVIEW)
        })
    }

    private companion object {
        const val FORK_URL = "https://github.com/ch7amada/Clock-Expressive"
    }
}
