import http from './http'
import type { PageVO } from '@/types/api'
import type { CheckInStatusVO, CreditBalanceVO, CreditTransactionVO } from '@/types/credit'

export function fetchCreditBalance() {
  return http.get<never, CreditBalanceVO>('/credits/balance')
}

export function fetchCreditTransactions(page = 1, size = 10) {
  return http.get<never, PageVO<CreditTransactionVO>>('/credits/transactions', { params: { page, size } })
}

export function fetchCheckInStatus() {
  return http.get<never, CheckInStatusVO>('/credits/check-in/today')
}

export function checkIn() {
  return http.post<never, CheckInStatusVO>('/credits/check-in')
}
