package com.vishnu.trim

object ScriptEngine {

    fun generateScript(serviceName: String, price: Double): String {
        val name = serviceName.lowercase()
        val formattedPrice = String.format("%.2f", price)

        // Utility/Telecom (High friction, high reward)
        if (name.contains("verizon") || name.contains("at&t") || name.contains("comcast") || name.contains("xfinity") || name.contains("spectrum")) {
            return """
                Hello, I am reviewing my monthly budget and my $serviceName bill at $$formattedPrice is currently too high. 
                
                I've been reviewing competitor offers in my area that provide similar speeds/service for significantly less. Before I transfer my service today, I wanted to check if I am eligible for any current retention promotions, loyalty discounts, or if we can restructure my plan to lower the rate.
                
                I would prefer to stay with you if we can make the pricing competitive. What can we do?
            """.trimIndent()
        } 
        
        // Default / Streaming (Low friction)
        return """
            Hi, I'm looking to cancel my $serviceName subscription today. The current price of $$formattedPrice/month no longer fits my budget. 
            
            Before I finalize the cancellation, are there any retention discounts, paused billing options, or lower-tier plans available to keep my account active? 
            
            If not, please proceed with the cancellation.
        """.trimIndent()
    }
}
