package com.example.azuretranslation;

import java.util.ArrayList;
import java.util.List;

class TranslatedText {
    // TODO: указать необходимые поля хранения ответа от API при переводе текста

    List<Translation> translations;

    static class Translation {
        String text, to;
    }

    @Override
    public String toString() {
        StringBuilder name = new StringBuilder();
        for (int i = 0; i < translations.size(); i++) {
            name.append(translations.get(i).text).append(System.lineSeparator());
        }
        return name.toString();
    }


    /*    [
    {
        "translations":[
        {"text":"你好, 你叫什么名字？","to":"zh-Hans"}
        ]
    }
]*/
}
