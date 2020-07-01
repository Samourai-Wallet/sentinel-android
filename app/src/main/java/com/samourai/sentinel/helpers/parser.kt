package com.samourai.sentinel.helpers

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule

val mapper = ObjectMapper().registerModule(KotlinModule())
