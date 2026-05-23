package com.example.data

data class NewsItem(
    val id: Int,
    val title: String,
    val summary: String,
    val category: String,
    val source: String,
    val time: String,
    val imageUrl: String
)

object NewsProvider {
    val allNews: List<NewsItem> by lazy {
        val categories = listOf("Tech", "Bharat", "Khel", "Finance", "Vigyan", "Yatra", "Duniya")
        val sources = listOf("News Express", "Bharat Today", "Naya Bharat", "Dainik Darpan", "Tech Sandesh", "Khel Mandli")
        
        // Let's generate 50 news items with high-quality hindi/english realistic headlines
        List(50) { index ->
            val id = index + 1
            val category = categories[index % categories.size]
            val source = sources[index % sources.size]
            val time = "${(index % 8) + 1} ghante pehle"
            
            val (title, summary) = when (index % 25) {
                0 -> Pair(
                    "ISRO ne launch kiya apna naya surveillance satellite",
                    "ISRO ne Shriharikota se ek naya surveillance aur earth observation satellite safaltapoorvak launch kiya hai. Isse krishi aur mausam vibhag ko behad madad milegi."
                )
                1 -> Pair(
                    "Smartphone market mein naye AI-powered phones ki dhoom",
                    "Is saal smartphone manufacturers ne fully-integrated local AI chipsets launch karne ka faisla kiya hai. Ab mobile bina internet sabhi heavy tasks kar sakega."
                )
                2 -> Pair(
                    "Cricket Team India ne haasil ki shandaar jeet",
                    "Aakhri over ke romanchak mukable mein Bharat ne Australia ko 4 wickets se harakar series apne naam kar li. Man of the match ne behtarin batting ki."
                )
                3 -> Pair(
                    "Ek naye start-up ne banayi sasti electric bike, single charge pe 150km",
                    "Bengaluru ke ek local electric vehicle start-up ne behad sasti aur aakarshak commuter bike launch ki hai jo general public ke liye kaafi affordable hai."
                )
                4 -> Pair(
                    "Stock Market touch kiya naya record high, investors mein khushi",
                    "BSE Sensex aur Nifty ne aaj subah sabse bada uchhale darj kiya. Global investors ki heavy buying aur strong policies ke chalte market gainer raha."
                )
                5 -> Pair(
                    "Sona aur Chandi ki keematon mein aayi bhaari girawat",
                    "Shadiyon ke season ke beech bullion market mein sone aur chandi ki keematon mein achanak sasti darj ki gayi hai, buy karne ka ye sahi samay bataya ja raha hai."
                )
                6 -> Pair(
                    "Delhi-Mumbai Express Highway ka naya phase shuru",
                    "Transport minister ne aaj high-speed highway ke naye section ka udghatan kiya. Isse dono bade shehron ke beech safar ka samay lagbhag 3 ghante kam ho jayega."
                )
                7 -> Pair(
                    "UNESCO ne declare kiya Bharat ke naye mandir ko World Heritage Site",
                    "UNESCO committee ne Bharat ke prachin mandir ke adbhut vaastukala aur saundarya ko dekhte hue use World Heritage Site ki list mein shamil kiya hai."
                )
                8 -> Pair(
                    "AI Robots ab ghar ke kaamon mein karenge madad",
                    "Japan ki ek robotics company ne naye autonomous companion robots ka demonstration kiya hai jo bartan dhone se lekar saaf-safai tak asani se kar sakte hain."
                )
                9 -> Pair(
                    "Garmiyon mein Uttarakhand ke in 5 secret places pe ghumne jayein",
                    "Agar aap bheed-bhaad se door thandi hawaon ka mazza lena chahte hain, toh Uttarakhand ki ye unexplored vadiyan aapke liye perfect weekend getaway hain."
                )
                10 -> Pair(
                    "Online fraud se bachne ke liye banko ne jaari ki nayi guidlines",
                    "Bank fraud ke badhte mamlo ko dekhte hue RBI ne sabhi customers se anurodh kiya hai ki apna OTP aur private pin kisi ke sath bhi share na karein."
                )
                11 -> Pair(
                    "Metaverse mein shuru hui digital shopping aur real-estate",
                    "Internet ki duniya ab badal rahi hai. Log ab metaverse mein apna virtual gully aur shop buy kar rahe hain jisme digital fashion brands launch ho rahe hain."
                )
                12 -> Pair(
                    "Board Exams ke results announced, padhein toppers ke success secrets",
                    "Is saal ke board pariksha nateeje ghoshit ho chuke hain. Toppers ne bataya ki unhone kadi mehnat ke sath-sath balanced schedule ko follow kiya tha."
                )
                13 -> Pair(
                    "Football Asia Cup mein Bharat ne kiya jabardast qualify",
                    "Bharatiya football team ne qualifying round mein behtarin khel dikhate hue finals ki ticket pakki kar li hai. Coach ne team ke dedication ki taarif ki."
                )
                14 -> Pair(
                    "NASA ke naye Telescope ne khoji prachin galaxy ki photo",
                    "Space Agency NASA ke telescope ne brahmand ke sabse prachin hisse se ek galaxy group ki tasveer post ki hai jo lagbhag 13 billion saal purani hai."
                )
                15 -> Pair(
                    "Coffee peene ke shaukeen saavdhan, naye study mein bada khulasa",
                    "Research se pata chala hai ki ek hadd se zyaada caffeine ka dugna sevan hamari sleep cycle ko disrupt karta hai aur mental stress badha sakta hai."
                )
                16 -> Pair(
                    "Electric trains ab chalengi 200 km/h ki speed se",
                    "Railway ministry dwara chalaye ja rahe premium track upgradation program ke tahat agle mahine se sleeper trains ki speed 200 tak badhai jayegi."
                )
                17 -> Pair(
                    "Indian food puri duniya mein sabse top trending list mein",
                    "Global culinary award ne Indian street food aur traditional cuisines ko sabse healthy aur flavorful ghoshit kiya. Biryani aur Butter Chicken top pe rahe."
                )
                18 -> Pair(
                    "Ayurveda se cancer ke ilaj pe nayi research ne di ummeed",
                    "Ayurvedic medicines aur modern therapy ke fusion se patients ke recovery rate mein kaafi sudhaar dekha gaya hai jo cancer se ladne mein madadgar hai."
                )
                19 -> Pair(
                    "Ghar pe koshish karein ye organic gardening tips",
                    "Apne terrace ya balcony pe asani se taazi sabjiyan ugane ke liye ye aasan aur bina kharche ke organic tips apnayein aur swasth rahein."
                )
                20 -> Pair(
                    "Gaming laptops ab hue dher saare saste",
                    "Naye graphics card ki production badhne se top brand gaming laptops ki price mein bhaari kami aayi hai, gamers ke liye khushkhabari."
                )
                21 -> Pair(
                    "Work From Home khatam karne ki taiyari mein baadi IT companies",
                    "Badi companies ne apne sabhi employees ko hafte mein minimum 4 din office aakar kaam karne ka naya order jaari kiya hai."
                )
                22 -> Pair(
                    "Naye Cyber Law se online bully karne walo ki khair nahi",
                    "Government ne naya cyber protection bill pass kiya hai jiske antargat kisi ko online abuse karne pe bhari fine aur jail ki saza ho sakti hai."
                )
                23 -> Pair(
                    "Agile methodology se badh rahi hai teams ki productivity",
                    "Corporate experts ke anusar chote goals banana aur sprint planning se employees ki delivery speed double ho chuki hai."
                )
                else -> Pair(
                    "Yoga asanas jo aapko rakhenge dinbhar energetic",
                    "Subah sirf 15 minute ke ye simple stretching aur yog asanas karne se aapka metabolic rate perfect rehta hai aur stress gayab ho jata hai."
                )
            }
            
            // Appending unique index markers to make sure each of the 100 items is recognizably unique
            val uniqueTitle = if (index >= 25) "$title ($id)" else title
            
            NewsItem(
                id = id,
                title = uniqueTitle,
                summary = "$summary Aaj ki taaza aur sabse badi khabar. Bane raho humare sath sabse pehle updates ke liye.",
                category = category,
                source = source,
                time = time,
                // Using consistent seed IDs for robust loading of unique pictures
                imageUrl = "https://picsum.photos/seed/news_pic_${(id % 30) + 1}/600/400"
            )
        }
    }
}
