#!/usr/bin/env python3
"""Single source for app strings. Run `python3 tools/strings.py` to regenerate
app/src/main/res/values/strings.xml (English) and values-pt-rBR/strings.xml (Brazilian Portuguese).
Every key must have both languages."""
import os
from xml.sax.saxutils import escape

S = {
    "app_name": ("Banca", "Banca"),
    "app_tagline": ("Your newsstand: calm, personal news.", "Sua banca: notícias com calma, do seu jeito."),

    # navigation
    "nav_home": ("Home", "Início"),
    "nav_saved": ("Saved", "Salvos"),
    "nav_settings": ("Settings", "Ajustes"),

    # home
    "greeting_morning": ("Good morning", "Bom dia"),
    "greeting_afternoon": ("Good afternoon", "Boa tarde"),
    "greeting_evening": ("Good evening", "Boa noite"),
    "search_hint": ("Search news and sources", "Buscar notícias e fontes"),
    "tab_for_you": ("For You", "Para você"),
    "tab_trending": ("Trending", "Em alta"),
    "tab_local": ("Local", "Local"),
    "section_markets": ("Markets", "Mercados"),
    "section_top": ("Top stories", "Destaques"),
    "section_market_news": ("Market news", "Notícias do mercado"),
    "section_for_you": ("Picked for you", "Escolhidas para você"),
    "section_topics": ("Topics", "Tópicos"),
    "section_sources": ("Sources", "Fontes"),
    "weather_feels": ("Feels like %1$s", "Sensação de %1$s"),
    "weather_today": ("Today", "Hoje"),
    "weather_manage": ("Manage places", "Gerenciar lugares"),
    "empty_title": ("Nothing here yet", "Nada por aqui ainda"),
    "empty_body": ("Pull down to refresh or follow more topics.", "Puxe para atualizar ou siga mais tópicos."),
    "empty_local": ("Add a place to see local news.", "Adicione um lugar para ver notícias locais."),
    "error_title": ("Couldn't load the news", "Não foi possível carregar as notícias"),
    "error_body": ("Check your connection and try again.", "Verifique sua conexão e tente de novo."),
    "loading": ("Loading…", "Carregando…"),

    # time
    "time_now": ("now", "agora"),
    "time_minutes": ("%1$d min", "%1$d min"),
    "time_hours": ("%1$d h", "%1$d h"),
    "time_days": ("%1$d d", "%1$d d"),

    # weather
    "wx_clear": ("Clear", "Céu limpo"),
    "wx_partly_cloudy": ("Partly cloudy", "Parcialmente nublado"),
    "wx_cloudy": ("Cloudy", "Nublado"),
    "wx_fog": ("Fog", "Neblina"),
    "wx_drizzle": ("Drizzle", "Garoa"),
    "wx_rain": ("Rain", "Chuva"),
    "wx_snow": ("Snow", "Neve"),
    "wx_showers": ("Showers", "Pancadas de chuva"),
    "wx_storm": ("Thunderstorm", "Tempestade"),

    # markets
    "market_low": ("Low", "Mínima"),
    "market_high": ("High", "Máxima"),
    "market_open": ("Start", "Início"),
    "market_delay_note": ("Quotes may be delayed by about 15 minutes.", "As cotações podem ter atraso de cerca de 15 minutos."),
    "market_currency": ("Currency", "Moeda"),
    "market_index": ("Index", "Índice"),
    "range_1d": ("1D", "1D"),
    "range_5d": ("5D", "5D"),
    "range_1m": ("1M", "1M"),
    "range_3m": ("3M", "3M"),
    "range_1y": ("1Y", "1A"),

    # topics
    "topic_world": ("World", "Mundo"),
    "topic_nation": ("Country", "País"),
    "topic_politics": ("Politics", "Política"),
    "topic_business": ("Business", "Economia"),
    "topic_technology": ("Technology", "Tecnologia"),
    "topic_science": ("Science", "Ciência"),
    "topic_sports": ("Sports", "Esportes"),
    "topic_entertainment": ("Entertainment", "Entretenimento"),
    "topic_health": ("Health", "Saúde"),
    "topic_games": ("Games", "Games"),
    "topic_cars": ("Cars", "Carros"),
    "topic_travel": ("Travel", "Viagem"),
    "topic_food": ("Food", "Gastronomia"),
    "topic_music": ("Music", "Música"),
    "topic_scitech": ("Science & Tech", "Ciência e Tecnologia"),
    "topic_top": ("Top stories", "Destaques"),
    "topic_markets": ("Markets", "Mercados"),

    # actions
    "action_save": ("Save", "Salvar"),
    "action_saved": ("Saved", "Salvo"),
    "action_unsave": ("Remove from saved", "Remover dos salvos"),
    "action_share": ("Share", "Compartilhar"),
    "action_more": ("More options", "Mais opções"),
    "action_hide": ("Hide", "Ocultar"),
    "sheet_show_less": ("Show less about", "Mostrar menos sobre"),
    "action_less_source": ("Show less from %1$s", "Mostrar menos de %1$s"),
    "action_block_source": ("Block %1$s", "Bloquear %1$s"),
    "action_follow": ("Follow %1$s", "Seguir %1$s"),
    "action_follow_source": ("Follow %1$s as a tab", "Seguir %1$s em uma aba"),
    "action_show_less": ("Show less", "Mostrar menos"),
    "action_mute_rule": ("Mute", "Silenciar"),
    "action_undo": ("Undo", "Desfazer"),
    "action_retry": ("Try again", "Tentar de novo"),
    "action_clear": ("Clear", "Limpar"),
    "action_clear_history": ("Clear history", "Limpar histórico"),
    "action_add_as_tab": ("Add as tab", "Criar aba"),
    "action_add_place": ("Add a place", "Adicionar lugar"),
    "action_add_tab": ("Add tab", "Adicionar aba"),
    "action_add_time": ("Add time", "Adicionar horário"),
    "action_add": ("Add", "Adicionar"),
    "action_edit": ("Edit", "Editar"),
    "action_back": ("Back", "Voltar"),
    "action_next": ("Next", "Continuar"),
    "action_skip": ("Skip", "Pular"),
    "action_block": ("Block", "Bloquear"),
    "action_unblock": ("Unblock", "Desbloquear"),
    "action_restore": ("Restore", "Restaurar"),
    "action_remove": ("Remove", "Remover"),
    "action_summarize": ("Summary", "Resumo"),
    "action_coverage": ("Coverage", "Cobertura"),
    "action_reader_view": ("Reader view", "Modo leitura"),
    "action_web_view": ("Open the page", "Abrir a página"),
    "action_open_browser": ("Open in browser", "Abrir no navegador"),
    "action_copy_link": ("Copy link", "Copiar link"),
    "action_text_size": ("Text size", "Tamanho do texto"),
    "action_reset_learning": ("Reset recommendations", "Zerar recomendações"),
    "action_unhide_all": ("Show hidden stories again", "Mostrar de novo as notícias ocultas"),
    "ok": ("OK", "OK"),
    "cancel": ("Cancel", "Cancelar"),
    "on": ("On", "Ativadas"),
    "off": ("Off", "Desativadas"),
    "none": ("None", "Nenhum"),
    "none_yet": ("Nothing yet", "Nada ainda"),
    "filter_all": ("All", "Todos"),

    # snackbars
    "msg_saved": ("Saved for later", "Salvo para ler depois"),
    "msg_removed_saved": ("Removed from saved", "Removido dos salvos"),
    "msg_hidden": ("Story hidden", "Notícia ocultada"),
    "msg_less_topic": ("You'll see less about %1$s", "Você verá menos sobre %1$s"),
    "msg_less_source": ("You'll see less from %1$s", "Você verá menos de %1$s"),
    "msg_blocked": ("%1$s blocked", "%1$s bloqueado"),
    "msg_followed": ("Following %1$s", "Seguindo %1$s"),
    "msg_tab_added": ("Tab \"%1$s\" added", "Aba \"%1$s\" criada"),
    "msg_link_copied": ("Link copied", "Link copiado"),
    "msg_learning_reset": ("Recommendations reset", "Recomendações zeradas"),
    "msg_unhidden": ("Hidden stories are back", "As notícias ocultas voltaram"),
    "msg_cache_cleared": ("Cache cleared", "Cache limpo"),
    "msg_exported": ("Settings exported", "Configurações exportadas"),
    "msg_export_failed": ("Couldn't export settings", "Não foi possível exportar"),
    "msg_imported": ("Settings imported", "Configurações importadas"),
    "msg_import_failed": ("This file isn't a Banca backup", "Este arquivo não é um backup do Banca"),
    "msg_permission_denied": ("Notifications are blocked for Banca in system settings", "As notificações do Banca estão bloqueadas nas configurações do sistema"),
    "msg_refreshing": ("Refreshing in the background", "Atualizando em segundo plano"),

    # explore
    "window_24h": ("Last 24 h", "Últimas 24 h"),
    "window_any": ("Any time", "Qualquer data"),
    "window_custom": ("Custom", "Personalizado"),
    "window_custom_title": ("Stories from the last…", "Notícias das últimas…"),
    "window_hours": ("%1$d h", "%1$d h"),
    "window_days": ("%1$d days", "%1$d dias"),
    "results_for": ("Results for \"%1$s\"", "Resultados para \"%1$s\""),
    "no_results": ("No results", "Nenhum resultado"),
    "no_results_body": ("Try other words or a wider time range.", "Tente outras palavras ou um período maior."),
    "recent_searches": ("Recent searches", "Buscas recentes"),

    # saved
    "saved_tab_saved": ("Saved", "Salvos"),
    "saved_tab_history": ("History", "Histórico"),
    "saved_empty_title": ("No saved stories", "Nenhuma notícia salva"),
    "saved_empty_body": ("Tap the bookmark on any story to read it later, even offline.", "Toque no marcador de qualquer notícia para ler depois, até offline."),
    "saved_search_hint": ("Search saved stories", "Buscar nos salvos"),
    "history_search_hint": ("Search history", "Buscar no histórico"),
    "inbox_title": ("Notifications", "Notificações"),
    "inbox_empty_title": ("Nothing here yet", "Nada por aqui ainda"),
    "inbox_empty_body": ("Digests and alerts you receive show up here, ready to open.", "Os resumos e alertas que você receber aparecem aqui, prontos para abrir."),
    "inbox_digest_at": ("Digest · %1$s", "Resumo · %1$s"),
    "inbox_alerts": ("Recent alerts", "Alertas recentes"),
    "history_empty_title": ("No history yet", "Nenhum histórico ainda"),
    "history_empty_body": ("Stories you open show up here.", "As notícias que você abrir aparecem aqui."),

    # reader
    "reader_minutes": ("%1$d min read", "%1$d min de leitura"),
    "reader_not_found": ("This story is no longer available.", "Esta notícia não está mais disponível."),
    "reader_unavailable": ("Reader view isn't available for this page.", "O modo leitura não está disponível para esta página."),
    "reader_read_on": ("Read on %1$s", "Ler em %1$s"),
    "reader_other_coverage": ("Other coverage", "Outras coberturas"),
    "coverage_none": ("No other outlets found for this story yet.", "Ainda não há outros veículos para esta notícia."),
    "summary_title": ("Summary", "Resumo"),
    "summary_from_publisher": ("From the publisher", "Do veículo"),
    "summary_key_points": ("Key points", "Pontos principais"),
    "summary_note": ("Made on your device from the article text. Check the full story for context.", "Feito no seu aparelho a partir do texto da matéria. Leia a matéria completa para o contexto."),
    "summary_unavailable": ("Couldn't summarize this page. Open the full story instead.", "Não foi possível resumir esta página. Abra a matéria completa."),
    "text_size_sample": ("The quick summary of the day's news, set in a comfortable size for reading.", "O resumo rápido das notícias do dia, num tamanho confortável para ler."),

    # settings home
    "group_feed": ("Feed", "Feed"),
    "group_cards": ("Cards", "Cartões"),
    "group_experience": ("Experience", "Experiência"),
    "group_app": ("App", "App"),
    "settings_tabs": ("Tabs", "Abas"),
    "settings_for_you": ("For You", "Para você"),
    "settings_for_you_sub": ("Blocks, topics and terms", "Blocos, tópicos e termos"),
    "settings_topics_sources": ("Topics and sources", "Tópicos e fontes"),
    "settings_topics_sources_sub": ("Hidden, shown less and blocked", "Ocultos, menos exibidos e bloqueados"),
    "settings_places": ("Places and weather", "Lugares e clima"),
    "settings_markets": ("Markets", "Mercados"),
    "settings_reading": ("Reading", "Leitura"),
    "settings_reading_sub": ("Reader view, fonts and summaries", "Modo leitura, fontes e resumos"),
    "settings_notifications": ("Notifications", "Notificações"),
    "settings_appearance": ("Appearance", "Aparência"),
    "settings_language": ("Language", "Idioma"),
    "settings_data": ("Data and storage", "Dados e armazenamento"),
    "settings_data_sub": ("Cache, images and backup", "Cache, imagens e backup"),
    "settings_about": ("About", "Sobre"),
    "settings_about_sub": ("Version, sources and licenses", "Versão, fontes e licenças"),
    "settings_edition": ("News country", "País das notícias"),
    "settings_edition_sub": ("The Google News country and language behind your tabs and searches. Places abroad, like Lisbon, automatically use their own country.", "País e idioma do Google News que alimentam suas abas e buscas. Lugares de outros países, como Lisboa, usam automaticamente o país deles."),
    "feed_age": ("Story age", "Idade das notícias"),
    "feed_age_sub": ("Older stories are hidden from tabs. Search is not affected.", "Notícias mais antigas somem das abas. A busca não é afetada."),
    "feed_age_label": ("Show stories up to", "Mostrar notícias de até"),
    "feed_age_any": ("Any age", "Qualquer idade"),
    "edition_br": ("Brazil", "Brasil"),
    "edition_pt": ("Portugal", "Portugal"),
    "edition_us": ("International (English)", "Internacional (inglês)"),

    # tabs
    "tabs_help": ("Reorder, remove or add tabs. Swipe between them on Home.", "Reordene, remova ou adicione abas. Deslize entre elas no Início."),
    "tabkind_for_you": ("Your mix", "Sua seleção"),
    "tabkind_trending": ("Most covered right now", "Mais cobertas agora"),
    "tabkind_local": ("Your places", "Seus lugares"),
    "tabkind_topic": ("Topic", "Tópico"),
    "tabkind_search": ("Search", "Busca"),
    "tabkind_feed": ("RSS feed", "Feed RSS"),
    "add_tab_search_label": ("Words to follow", "Palavras para seguir"),
    "add_tab_search_help": ("Google News operators work: \"exact phrase\", site:g1.globo.com, -word.", "Operadores do Google News funcionam: \"frase exata\", site:g1.globo.com, -palavra."),
    "add_tab_feed_name": ("Name", "Nome"),
    "add_tab_feed_url": ("Feed or site address", "Endereço do feed ou site"),

    # for you
    "foryou_help": ("Choose the blocks shown on For You and their order.", "Escolha os blocos do Para você e a ordem deles."),
    "block_weather": ("Weather", "Clima"),
    "block_markets": ("Markets", "Mercados"),
    "block_top": ("Top stories", "Destaques"),
    "block_market_news": ("Market news", "Notícias do mercado"),
    "block_feed": ("Your feed", "Seu feed"),
    "foryou_mix": ("Topics in your mix", "Tópicos da sua seleção"),
    "foryou_mix_sub": ("For You blends these topics with what you read most.", "O Para você mistura estes tópicos com o que você mais lê."),
    "foryou_terms": ("Followed terms", "Termos seguidos"),
    "foryou_terms_sub": ("Stories mentioning these words join For You.", "Notícias com estas palavras entram no Para você."),
    "foryou_terms_hint": ("e.g. Cruzeiro, AI", "ex.: Cruzeiro, IA"),

    # topics & sources
    "topics_shown_less": ("Topics shown less", "Tópicos exibidos menos"),
    "topics_shown_less_sub": ("To add one, open the ⋮ menu of any story (or long-press it) and pick a topic, name or outlet under \"Show less about\".", "Para adicionar, abra o menu ⋮ de uma notícia (ou toque e segure nela) e escolha um tópico, nome ou veículo em \"Mostrar menos sobre\"."),
    "sources_blocked_less": ("Blocked and muted sources", "Fontes bloqueadas e silenciadas"),
    "sources_blocked_less_sub": ("Blocked sources never appear. Muted ones appear less.", "Fontes bloqueadas nunca aparecem. As silenciadas aparecem menos."),
    "source_blocked": ("Blocked", "Bloqueada"),
    "source_less": ("Shown less", "Exibida menos"),
    "learning": ("Recommendations", "Recomendações"),
    "learning_sub": ("Banca learns from what you open and save, only on this device.", "O Banca aprende com o que você abre e salva, só neste aparelho."),

    # places
    "places_help": ("The first place is used by the weather chip. Local shows news from all of them.", "O primeiro lugar aparece no clima do topo. A aba Local mostra notícias de todos."),
    "places_add": ("Add a place", "Adicionar lugar"),
    "places_search_hint": ("City, region or country", "Cidade, região ou país"),
    "places_use_location": ("Use my location", "Usar minha localização"),
    "places_my_location": ("My location", "Minha localização"),
    "units": ("Units", "Unidades"),
    "units_temperature": ("Temperature", "Temperatura"),
    "units_wind": ("Wind", "Vento"),

    # markets settings
    "markets_help": ("Shown in the market strip on For You.", "Exibidos na faixa de mercados do Para você."),
    "markets_available": ("Available", "Disponíveis"),
    "markets_display": ("Display", "Exibição"),
    "markets_change_percent": ("Show change as percentage", "Mostrar variação em porcentagem"),

    # reading settings
    "reading_preview": ("Preview", "Prévia"),
    "reading_reader": ("Reader", "Leitor"),
    "reading_reader_default": ("Open stories in reader view", "Abrir notícias no modo leitura"),
    "reading_reader_default_sub": ("Clean text without ads; switch to the page anytime.", "Texto limpo, sem anúncios; troque para a página quando quiser."),
    "reading_font": ("Article font", "Fonte da matéria"),
    "font_serif": ("Serif", "Serifada"),
    "font_sans": ("Sans", "Sem serifa"),
    "reading_summaries": ("Summaries", "Resumos"),
    "reading_summaries_sub": ("Summaries use the publisher's own text when available, otherwise key sentences, skipping clickbait.", "Os resumos usam o texto do próprio veículo quando existe; senão, as frases-chave, sem chamadas sensacionalistas."),
    "reading_feed_summaries": ("Summaries under headlines", "Resumo abaixo das manchetes"),
    "reading_feed_summaries_sub": ("Two lines from the publisher in the feed", "Duas linhas do veículo no feed"),
    "reading_summary_length": ("Summary length", "Tamanho do resumo"),
    "reading_summary_points": ("%1$d points", "%1$d pontos"),

    # notifications
    "channel_alerts": ("Big stories", "Grandes notícias"),
    "channel_digest": ("Digest", "Resumo do dia"),
    "channel_following": ("Following", "Seguindo"),
    "notif_big_story": ("Big story", "Grande notícia"),
    "notif_digest_title": ("Your news digest", "Seu resumo de notícias"),
    "notif_digest_summary": ("%1$d stories", "%1$d notícias"),
    "notif_enable": ("Allow notifications", "Permitir notificações"),
    "notif_enable_sub": ("Alerts and digests from what you follow", "Alertas e resumos do que você segue"),
    "notif_following": ("What to notify", "O que notificar"),
    "notif_following_sub": ("Each rule has its own delivery, style and Android channel.", "Cada regra tem sua entrega, estilo e canal do Android."),
    "notif_big_stories": ("Big stories", "Grandes notícias"),
    "notif_big_stories_sub": ("Stories covered by many outlets", "Notícias cobertas por muitos veículos"),
    "notif_add_rule": ("Follow a topic, term, source or place", "Seguir tópico, termo, fonte ou lugar"),
    "notif_rule": ("Rule", "Regra"),
    "notif_digest": ("Digest", "Resumo"),
    "notif_digest_sub": ("One grouped notification with the top stories at the times you choose.", "Uma notificação agrupada com os destaques nos horários que você escolher."),
    "notif_digest_enable": ("Send digests", "Enviar resumos"),
    "notif_digest_times": ("Times", "Horários"),
    "notif_digest_weather": ("Include weather", "Incluir o clima"),
    "notif_limits": ("Limits", "Limites"),
    "notif_quiet": ("Quiet hours", "Horário de silêncio"),
    "notif_quiet_sub": ("%1$s to %2$s; alerts wait for the next digest", "%1$s às %2$s; os alertas esperam o próximo resumo"),
    "notif_quiet_from": ("From %1$s", "De %1$s"),
    "notif_quiet_to": ("To %1$s", "Até %1$s"),
    "notif_quiet_urgent": ("Let urgent big stories through", "Deixar passar grandes notícias urgentes"),
    "notif_daily_limit": ("Maximum per day", "Máximo por dia"),
    "notif_refresh": ("Background refresh", "Atualização em segundo plano"),
    "notif_refresh_sub": ("Android runs this at most every 15 minutes and may batch it to save battery.", "O Android roda isso no máximo a cada 15 minutos e pode agrupar para poupar bateria."),
    "notif_refresh_every": ("Check for news", "Buscar notícias"),
    "refresh_manual": ("Manual only", "Só manual"),
    "refresh_minutes": ("Every %1$d min", "A cada %1$d min"),
    "refresh_hours": ("Every %1$d h", "A cada %1$d h"),
    "notif_wifi_only": ("Only on Wi-Fi", "Só no Wi-Fi"),
    "notif_charging_only": ("Only while charging", "Só carregando"),
    "notif_battery": ("Pause when battery is low", "Pausar com bateria fraca"),
    "notif_refresh_now": ("Refresh now", "Atualizar agora"),
    "notif_system_settings": ("Android notification settings", "Notificações nas configurações do Android"),
    "rule_kind_topic": ("Topic", "Tópico"),
    "rule_kind_term": ("Term", "Termo"),
    "rule_kind_source": ("Source", "Fonte"),
    "rule_kind_place": ("Place", "Lugar"),
    "rule_no_places": ("Add places in Places and weather first.", "Adicione lugares em Lugares e clima primeiro."),
    "rule_term_hint": ("Words or \"exact phrase\"", "Palavras ou \"frase exata\""),
    "rule_source_hint": ("Site, e.g. g1.globo.com", "Site, ex.: g1.globo.com"),
    "rule_delivery": ("Delivery", "Entrega"),
    "delivery_instant": ("Instant", "Na hora"),
    "delivery_digest": ("In digest", "No resumo"),
    "delivery_off": ("Off", "Desligada"),
    "rule_big_only": ("Only big stories", "Só grandes notícias"),
    "rule_big_only_sub": ("Covered by at least 4 outlets", "Cobertas por pelo menos 4 veículos"),
    "rule_style": ("Style", "Estilo"),
    "rule_style_sub": ("How each notification looks in the shade.", "Como cada notificação aparece na barra."),
    "style_large": ("Large image", "Imagem grande"),
    "style_compact": ("Compact", "Compacta"),
    "style_headline": ("Headline", "Só título"),
    "rule_sound": ("Sound and vibration", "Som e vibração"),
    "rule_sound_sub": ("Opens this rule's Android channel", "Abre o canal desta regra no Android"),
    "rule_delete": ("Delete rule", "Excluir regra"),
    "rule_deleted": ("This rule was deleted.", "Esta regra foi excluída."),

    # appearance
    "appearance_theme": ("Theme", "Tema"),
    "theme_system": ("System", "Sistema"),
    "theme_light": ("Light", "Claro"),
    "theme_dark": ("Dark", "Escuro"),
    "appearance_amoled": ("Pure black", "Preto puro"),
    "appearance_amoled_sub": ("Dark theme on true black (AMOLED)", "Tema escuro em preto total (AMOLED)"),
    "appearance_dynamic": ("Wallpaper colours", "Cores do papel de parede"),
    "appearance_dynamic_sub": ("Use Material You colours instead of Banca blue", "Usar as cores do Material You no lugar do azul do Banca"),
    "appearance_motion": ("Motion", "Movimento"),
    "appearance_motion_sub": ("Animations also follow the Android \"Remove animations\" setting.", "As animações também seguem a opção \"Remover animações\" do Android."),
    "appearance_reduce_motion": ("Reduce motion", "Reduzir movimento"),

    # data
    "data_usage": ("Usage", "Uso"),
    "data_images_wifi": ("Pictures only on Wi-Fi", "Imagens só no Wi-Fi"),
    "data_images_wifi_sub": ("Saves mobile data", "Economiza dados móveis"),
    "data_clear_cache": ("Clear cache", "Limpar cache"),
    "data_cache_size": ("About %1$d MB", "Cerca de %1$d MB"),
    "data_backup": ("Backup", "Backup"),
    "data_backup_sub": ("Settings, tabs, rules and blocked sources in a JSON file.", "Ajustes, abas, regras e fontes bloqueadas em um arquivo JSON."),
    "data_export": ("Export settings", "Exportar configurações"),
    "data_import": ("Import settings", "Importar configurações"),

    # language
    "language_sub": ("Changes the app's language. News follow the edition set in Tabs.", "Muda o idioma do app. As notícias seguem a edição escolhida em Abas."),

    # about
    "about_version": ("Version %1$s (build %2$d)", "Versão %1$s (build %2$d)"),
    "about_source": ("Source code", "Código-fonte"),
    "about_privacy": ("Privacy", "Privacidade"),
    "about_privacy_sub": ("No account, no tracking. Everything stays on this device.", "Sem conta e sem rastreamento. Tudo fica neste aparelho."),
    "about_data_sources": ("Data sources", "Fontes de dados"),
    "about_data_sources_sub": ("Google News RSS, publishers' feeds, Open-Meteo, Yahoo Finance, AwesomeAPI", "RSS do Google News, feeds dos veículos, Open-Meteo, Yahoo Finance, AwesomeAPI"),
    "about_licenses": ("Open-source licenses", "Licenças de código aberto"),

    # onboarding
    "onboarding_welcome": ("Welcome to Banca", "Boas-vindas ao Banca"),
    "onboarding_welcome_body": ("Your newsstand: the day's news from many sources, calm and organized your way.", "Sua banca: as notícias do dia de várias fontes, com calma e do seu jeito."),
    "onboarding_topics": ("What interests you?", "O que te interessa?"),
    "onboarding_topics_body": ("Pick at least three. You can change this anytime.", "Escolha pelo menos três. Dá para mudar quando quiser."),
    "onboarding_places": ("Your places", "Seus lugares"),
    "onboarding_places_body": ("For local news and the weather card.", "Para notícias locais e o cartão de clima."),
    "onboarding_markets": ("Markets", "Mercados"),
    "onboarding_markets_body": ("Currencies and indices on your For You page.", "Moedas e índices na sua página Para você."),
    "onboarding_notifications": ("Stay in the loop", "Fique por dentro"),
    "onboarding_notifications_body": ("Choose how Banca reaches you.", "Escolha como o Banca chega até você."),
    "onboarding_notif_off": ("No notifications", "Sem notificações"),
    "onboarding_notif_off_body": ("Open the app when you want", "Abra o app quando quiser"),
    "onboarding_notif_digest": ("Daily digest", "Resumo diário"),
    "onboarding_notif_digest_body": ("The top stories at set times", "Os destaques em horários definidos"),
    "onboarding_notif_big": ("Big stories only", "Só grandes notícias"),
    "onboarding_notif_big_body": ("When many outlets cover the same story", "Quando muitos veículos cobrem a mesma notícia"),
    "onboarding_notif_later": ("Follow topics, terms and places later in Settings › Notifications.", "Siga tópicos, termos e lugares depois em Ajustes › Notificações."),
    "onboarding_start": ("Start reading", "Começar a ler"),
}

PLURALS = {
    "more_sources": ({"one": "+%d source", "other": "+%d sources"}, {"one": "+%d fonte", "other": "+%d fontes"}),
    "sources_count": ({"one": "%d source", "other": "%d sources"}, {"one": "%d fonte", "other": "%d fontes"}),
    "tabs_count": ({"one": "%d tab", "other": "%d tabs"}, {"one": "%d aba", "other": "%d abas"}),
    "items_count": ({"one": "%d item", "other": "%d items"}, {"one": "%d item", "other": "%d itens"}),
}


def esc(s: str) -> str:
    s = escape(s)
    s = s.replace("'", "\\'").replace('"', '\\"')
    return s


def write(lang_index: int, path: str):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    out = ['<?xml version="1.0" encoding="utf-8"?>', "<resources>"]
    for k, v in S.items():
        attr = ' translatable="false"' if k == "app_name" and lang_index == 0 else ""
        if k == "app_name" and lang_index == 1:
            continue
        out.append(f'    <string name="{k}"{attr}>{esc(v[lang_index])}</string>')
    for k, v in PLURALS.items():
        out.append(f'    <plurals name="{k}">')
        for q, t in v[lang_index].items():
            out.append(f'        <item quantity="{q}">{esc(t)}</item>')
        out.append("    </plurals>")
    out.append("</resources>")
    with open(path, "w", encoding="utf-8") as f:
        f.write("\n".join(out) + "\n")


if __name__ == "__main__":
    root = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "app", "src", "main", "res")
    write(0, os.path.join(root, "values", "strings.xml"))
    write(1, os.path.join(root, "values-pt-rBR", "strings.xml"))
    print(f"{len(S)} strings, {len(PLURALS)} plurals")
