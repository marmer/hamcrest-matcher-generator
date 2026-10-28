package model

data class Customer(val name: String)

data class Order(val id: Long, val customer: Customer)
