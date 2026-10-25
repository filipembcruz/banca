<p align="center">
  <img src="docs/icon.png" width="112" alt="Ícone do Banca">
</p>

<h1 align="center">Banca</h1>

<p align="center">
  <b>Sua banca de notícias.</b> Um leitor de notícias calmo e privado para Android.<br>
  Google News e feeds dos veículos num feed só seu, com clima, moedas e mercados.<br>
  Sem conta, sem anúncios, sem rastreamento.
</p>

<p align="center">
  <a href="https://github.com/filipembcruz/banca/releases/latest/download/banca.apk"><b>⬇ Baixar o APK mais recente</b></a>
  &nbsp;·&nbsp;
  <a href="README.md">Read in English</a>
</p>

<p align="center">
  <img src="docs/screens/home_light.png" width="240" alt="Início, tema claro">
  <img src="docs/screens/home_dark.png" width="240" alt="Início, tema escuro">
  <img src="docs/screens/reader.png" width="240" alt="Modo leitura">
</p>

## Recursos

**Seu feed**
- **Para você** mistura os tópicos que você escolhe com o que você realmente lê. O aprendizado fica só no aparelho.
- **Em alta** mostra primeiro as notícias cobertas por mais veículos agora.
- **Local** tem um feed por lugar (Belo Horizonte, Lisboa…), trocado por chips, na ordem que você definir.
- **Abas suas:** qualquer tópico, uma busca salva (com operadores do Google News como `site:` ou `"frase exata"`) ou qualquer feed RSS.
- Cada notícia mostra o veículo principal primeiro, com as **outras coberturas** da mesma história a um toque.

**Leitura**
- **Modo leitura** limpo, com fonte serifada, imagem de ponta a ponta com parallax, tamanho de texto ajustável e progresso de leitura.
- A **página original** abre num navegador dentro do app quando você preferir.
- **Resumos offline:** o resumo do próprio veículo quando o feed traz um; senão, as frases-chave da matéria. Chamadas sensacionalistas são filtradas. Sem serviço de IA e sem chave de API.
- **Imagens com zoom:** toque em qualquer imagem e use pinça ou dois toques.

**Cartões do Para você**
- **Clima** para cada lugar, compacto ou expandido, com sol, nuvens, chuva, neve e raios animados. Dados do Open-Meteo.
- **Mercados:** moedas (USD/BRL, EUR/BRL…) e índices (Ibovespa, S&P 500, Euro Stoxx 50…) com minigráficos e um gráfico detalhado.
- **Notícias do mercado** dos índices que você acompanha.

**Você no controle**
- Salve notícias para ler depois, até offline. Salvos e histórico têm busca.
- **Oculte** uma notícia ou **mostre menos** sobre um tópico, um nome do título ou um veículo. Tudo pode ser desfeito e revertido nos Ajustes.
- **Bloqueie** veículos por completo.
- Reordene abas, lugares, mercados e blocos do Para você arrastando.
- Escolha o país das notícias (Brasil, Portugal ou internacional) e a idade máxima das notícias.

**Notificações**
- Regras por tópico, termo, veículo ou lugar, cada uma com sua entrega (na hora ou no resumo), estilo (imagem grande, compacta ou só título) e canal do Android.
- **Resumos** nos horários que você escolher, com o clima se quiser. Ao tocar, abre uma caixa de entrada com todas as notícias prontas para ler.
- Alertas de **grandes notícias**, horário de silêncio, limite diário e atualização em segundo plano que poupa bateria.

**Visual**
- Temas claro, escuro e preto puro, cor de destaque fixa ou do papel de parede (Material You).
- Transições com elementos compartilhados, animações com mola e barra de navegação flutuante. Respeita o "Remover animações" do Android.
- Inglês e português do Brasil.

## Mais capturas

<p align="center">
  <img src="docs/screens/local.png" width="200" alt="Feed">
  <img src="docs/screens/reader_dark.png" width="200" alt="Leitura carregando, escuro">
  <img src="docs/screens/onboarding.png" width="200" alt="Tópicos no onboarding">
  <img src="docs/screens/settings.png" width="200" alt="Ajustes">
</p>

## Instalação

1. No celular, baixe o [`banca.apk`](https://github.com/filipembcruz/banca/releases/latest/download/banca.apk) da última versão.
2. Abra o arquivo e permita a instalação pelo navegador ou gerenciador de arquivos se o Android pedir.
3. Se o Play Protect avisar sobre app desconhecido, escolha **Instalar mesmo assim**. O Banca só pede notificações e, se você quiser, sua localização aproximada.

Novas versões instalam por cima e mantêm seus dados. Requer Android 8.0 ou mais recente.

## Privacidade

O Banca não tem conta, analytics nem servidor próprio. Ajustes, salvos, histórico e o que o app aprende sobre seus interesses ficam no seu celular. O app só se comunica com as fontes que lê: RSS do Google News, sites e feeds dos veículos, Open-Meteo (clima), Yahoo Finance e AwesomeAPI (cotações) e o serviço de favicons do Google (ícones dos veículos). Dá para exportar e importar os ajustes num arquivo JSON.

## Compilar

Projeto Gradle com Kotlin e Jetpack Compose, sem wrapper versionado.

```bash
# Gradle 8.11+ e JDK 17, Android SDK 35
gradle assembleDebug
# capturas das telas principais (Robolectric + Roborazzi), em app/build/screens
gradle :app:recordRoborazziDebug
```

Cada push na `main` roda o [workflow do GitHub Actions](.github/workflows/android.yml): compila, assina e publica uma release com o APK e as capturas.

## Licença

[MIT](LICENSE)
