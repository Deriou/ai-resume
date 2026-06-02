export interface CreditBalanceVO {
  balance: number
}

export interface CheckInStatusVO {
  checkedIn: boolean
  date: string
  creditReward: number
  balance: number
}

export interface CreditTransactionVO {
  id: number
  userId: number
  changeAmount: number
  type: string
  balanceAfter: number
  refType: string | null
  refId: number | null
  remark: string | null
  createdAt: string
}
