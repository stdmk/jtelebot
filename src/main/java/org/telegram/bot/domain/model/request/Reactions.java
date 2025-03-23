package org.telegram.bot.domain.model.request;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

@Data
@Accessors(chain = true)
public class Reactions {
    List<String> newEmojis = new ArrayList<>();
    List<String> oldEmojis = new ArrayList<>();
    List<String> newCustomEmojisIds = new ArrayList<>();
    List<String> oldCustomEmojisIds = new ArrayList<>();
}
