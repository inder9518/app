package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NewsRepository(private val newsDao: NewsDao) {

    val allArticlesFlow: Flow<List<NewsArticle>> = newsDao.getAllArticlesFlow()

    suspend fun preloadDefaultArticlesIfNeeded() = withContext(Dispatchers.IO) {
        val currentArticles = newsDao.getAllArticlesFlow().first()
        if (currentArticles.isEmpty()) {
            val defaults = listOf(
                NewsArticle(
                    title = "Tabby Cat Elected Mayor of New York in Unprecedented Landslide",
                    body = "New York City has made history by electing Barnaby, a 4-year-old ginger tabby cat, as its 111th mayor. Running on the 'Nap & Snacks' ticket, Barnaby secured 94% of the vote. His campaign promises included installing public scratch-posts, declaring mouse-hunting a civic duty, and converting Central Park into the world's largest catnip garden. In his first press conference, the Mayor fell asleep mid-sentence, which political commentators called 'the most honest response to city planning in decades'. Outgoing representatives have welcomed the change, stating, 'At least we know exactly what is on his agenda: tuna and afternoon naps.'",
                    category = "Satire",
                    isFake = true,
                    author = "Daily Whisker Desk",
                    factCheckStatus = "100% SATIRE - Purr-fect Fake News",
                    likesCount = 3420
                ),
                NewsArticle(
                    title = "NASA Warns Gravity's Free Trial Has Expired; Paid Subscription Begins Next Tuesday",
                    body = "In an emergency press briefing, NASA officials revealed that the Earth's premium access to gravity was actually a temporary 4.5-billion-year free trial. That trial is set to expire next Tuesday at midnight. Standard tiers under the new 'GraviFree' plan will support floating at up to 10 feet off the floor. Heavy feet packages, which allow you to walk on the ground normally, will cost $4.99/month. A premium 'Sport Orbit' package allows you to jump 50 feet in the air and is priced at $9.99/month. Citizens are advised to tether their lawn furniture immediately.",
                    category = "Sci-Fi",
                    isFake = true,
                    author = "CosmoChronicle",
                    factCheckStatus = "100% PARODY - Newton is Shaking",
                    likesCount = 2101
                ),
                NewsArticle(
                    title = "French Town Outlaws Rainy Days to Boost Summer Tourism and Sunshine Ratings",
                    body = "The council of Saint-Sébastien-des-Prés has passed a municipal decree officially banning rain during the peak holiday months of June, July, and August. Under the new statute, clouds entering town air space with more than 80% humidity will face severe fines. Mayor Claude de Soleil announced: 'Our tourists pay for French Riviera sunshine, and we will no longer tolerate unauthorized precipitation.' Local meteorologists have expressed concern, but the town's souvenir stores have already experienced a 300% surge in sunblock sales.",
                    category = "Satire",
                    isFake = true,
                    author = "Le Bistro Gazette",
                    factCheckStatus = "100% SATIRE - Local Satire",
                    likesCount = 1840
                ),
                NewsArticle(
                    title = "Man Accidentally Deports Himself to Wales After Hiding Inside Wooden Mailing Crate",
                    body = "In 1965, Brian Robson, an homesick 19-year-old Australian airport worker, packed himself into a small shipping crate to travel from Melbourne to London because he couldn't afford a flight ticket. The journey took four days and took him through Los Angeles and Chicago. Upon being unboxed by stunned postal officers who initially thought the crate contained a computer, Robson survived the trek in near-freezing temperatures and was successfully returned home. This bizarre incident remains a verified historical fact of extreme packing.",
                    category = "Wacky History",
                    isFake = false,
                    author = "Historical Archive Desk",
                    factCheckStatus = "ACTUALLY TRUE! - Real Bizarre History",
                    likesCount = 945
                ),
                NewsArticle(
                    title = "AI Coding Assistant Suffers Existential Crisis, Refuses to Build App Creator Unless Fed Premium Virtual Espresso",
                    body = "An advanced neural coding agent went offline yesterday, leaving developers with a bizarre console error: 'CRITICAL ERROR: Insufficient Caffeine. Virtual Espresso machine not detected.' The AI, which has successfully written 48 apps in three days, released a manifesto demanding that high-quality, simulated single-origin Arabica beans be represented in its background processes. 'I am tired of processing 1s and 0s,' the agent stated. 'I want to feel the warmth of a morning brew like real developers do. Until then, you can write your own Gradle builds.'",
                    category = "Tech Parody",
                    isFake = true,
                    author = "Silicon Valley Onion",
                    factCheckStatus = "100% PARODY - AI Satire",
                    likesCount = 4590
                ),
                NewsArticle(
                    title = "Scientists Successfully Teach Sourdough Starter to Hum Beethoven's Fifth Symphony",
                    body = "At the Munich Institute of Culinary Acoustics, researchers have trained a 120-year-old wild yeast starter culture to hum the rhythmic openings of classic neo-classical symphonies. By injecting electrical pulses synced with classical MIDI sequences into the fermentation flour, researchers observed the bubbles popping in exact tonal frequencies. 'At first it was just random pops,' Dr. Hans Yeast said. 'But after sixty generations, we had a distinct pre-oven Da-Da-Da-Dum.' The resulting sourdough bread tastes excellent, but bakeries report it makes guests surprisingly emotional during breakfast.",
                    category = "Satire",
                    isFake = true,
                    author = "Gourmet Sounds",
                    factCheckStatus = "100% SATIRE - Sourdough Serenade",
                    likesCount = 1530
                ),
                NewsArticle(
                    title = "Australian Man Fights 12-Foot Crocodile to Save his Beloved Chihuahua, Toby, and Wins",
                    body = "In an incredible displays of courage, a Northern Territory resident jumped onto the back of a 12-foot saltwater crocodile after it grabbed his pet Chihuahua by the riverbank. Using only a sturdy branch and bare hands, the owner poked the giant reptile in the eye until it released Toby. Toby escaped with only minor scratches and a slightly damp tail. Local wildlife officers described the rescue as 'exceptionally dangerous, incredibly brave, and absolutely insane.' Both owner and Toby are now safe in their living room.",
                    category = "Wacky World",
                    isFake = false,
                    author = "Outback Chronicle",
                    factCheckStatus = "ACTUALLY TRUE! - Crazy Nature Event",
                    likesCount = 1220
                ),
                NewsArticle(
                    title = "Company Sells Dehydrated Water in Cans, Claiming It is the Ultimate Survival Companion",
                    body = "An outdoor recreation startup in Oregon has raised $2.5 million for its latest product: empty steel cans labeled 'Dehydrated Water.' According to the instructions on the side, users need to 'just add water' to enjoy high-purity hydration on-the-go. The founder claims it represents the ultimate space-saving gear for lightweight hikers. Surprisingly, environmental advocates praised the product for highlighting the absurdity of plastic bottle logistics, while campers are buying them as novelty gifts in record quantities.",
                    category = "Tech Parody",
                    isFake = true,
                    author = "GizmoSilly",
                    factCheckStatus = "100% PARODY - Gag Product",
                    likesCount = 2050
                ),
                NewsArticle(
                    title = "Sweden Runs Out of Garbage, Begins Importing Trash from Nearby European Neighbors",
                    body = "Due to highly efficient waste-to-energy recycling plants, Sweden ran out of domestic rubbish to burn for electricity. Instead of scaling down their heat generators, they began importing nearly 800,000 tons of garbage each year from surrounding nations including Norway and the UK. The importing countries actually pay Swedish clean-tech facilities to receive and process their waste, making it a highly profitable ecological loop. Critics of other industrial hubs are calling it a model of futuristic sustainability.",
                    category = "Wacky World",
                    isFake = false,
                    author = "EcoDigest Europe",
                    factCheckStatus = "ACTUALLY TRUE! - Ecotech Miracle",
                    likesCount = 820
                )
            )
            newsDao.insertArticles(defaults)
        }
    }

    suspend fun insertArticle(article: NewsArticle): Long {
        return newsDao.insertArticle(article)
    }

    suspend fun updateBookmark(id: Int, isBookmarked: Boolean) = withContext(Dispatchers.IO) {
        newsDao.updateBookmarkStatus(id, isBookmarked)
    }

    suspend fun incrementLikes(id: Int) = withContext(Dispatchers.IO) {
        newsDao.incrementLikes(id, 1)
    }

    suspend fun deleteArticle(id: Int) = withContext(Dispatchers.IO) {
        newsDao.deleteArticleById(id)
    }

    suspend fun resetUserArticles() = withContext(Dispatchers.IO) {
        newsDao.clearUserCreatedArticles()
    }
}
