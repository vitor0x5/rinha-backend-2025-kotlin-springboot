package com.vitor.gui.rinhakotlinspring

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class RinhaKotlinSpringApplication

fun main(args: Array<String>) {
    runApplication<RinhaKotlinSpringApplication>(*args)
}
