# Firestore document examples

## users/{uid}
{
"username": "alfonso",
"zodiacSign": "leo",
"level": 3,
"xp": 420,
"createdAt": "<timestamp>",
"updatedAt": "<timestamp>"
}

## posts/{postId}
{
"authorId": "<uid>",
"text": "Hoy he tenido un sueño raro... ¿alguien más?",
"topic": "energy",
"createdAt": "<timestamp>",
"updatedAt": "<timestamp>",
"likeCount": 4,
"commentCount": 2,
"status": "active"
}

## posts/{postId}/comments/{commentId}
{
"authorId": "<uid>",
"text": "A mí me pasa cuando estoy cargado de energía.",
"createdAt": "<timestamp>",
"status": "active"
}

## posts/{postId}/reactions/{uid}
{
"type": "like",
"createdAt": "<timestamp>"
}
