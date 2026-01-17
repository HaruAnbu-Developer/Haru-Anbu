from twilio.rest import Client

# Twilio 콘솔에서 복사
account_sid = ''
auth_token = ''
client = Client(account_sid, auth_token)

call = client.calls.create(
    # twiml='<Response><Say language="ko-KR">안녕하세요, 테스트입니다.</Say></Response>',
    url="http://demo.twilio.com/docs/voice.xml",
    to='+', # 김준수 죽어라
    from_='+'
)

print(f"통화 요청 성공! SID: {call.sid}")