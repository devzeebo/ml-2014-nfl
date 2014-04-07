package edu.zeebo.ml.nfl

/**
 * User: Eric Siebeneich
 * Date: 4/7/14
 */
class CSVMerger {

	File rawDir = new File('raw')
	File cleanDir = new File('clean')

	def teamNamesToAbbr = [
			'Atlanta Falcons' : 'atl',
			'Arizona Cardinals' : 'crd',
			'Baltimore Colts' : 'clt',
			'Baltimore Ravens' : 'rav',
			'Buffalo Bills' : 'buf',
			'Carolina Panthers' : 'car',
			'Chicago Bears' : 'chi',
			'Cincinnati Bengals' : 'cin',
			'Cleveland Browns' : 'cle',
			'Dallas Cowboys' : 'dal',
			'Denver Broncos' : 'den',
			'Detroit Lions' : 'det',
			'Green Bay Packers' : 'gnb',
			'Houston Oilers' : 'oti',
			'Houston Texans' : 'htx',
			'Indianapolis Colts' : 'clt',
			'Jacksonville Jaguars' : 'jax',
			'Kansas City Chiefs' : 'kan',
			'Los Angeles Raiders' : 'rai',
			'Los Angeles Rams' : 'ram',
			'Miami Dolphins' : 'mia',
			'Minnesota Vikings' : 'min',
			'New England Patriots' : 'nwe',
			'New Orleans Saints' : 'nor',
			'New York Giants' : 'nyg',
			'New York Jets' : 'nyj',
			'Oakland Raiders' : 'rai',
			'Philadelphia Eagles' : 'phi',
			'Phoenix Cardinals' : 'crd',
			'Pittsburgh Steelers' : 'pit',
			'San Diego Chargers' : 'sdg',
			'San Francisco 49ers' : 'sfo',
			'Seattle Seahawks' : 'sea',
			'St. Louis Cardinals' : 'crd',
			'St. Louis Rams' : 'ram',
			'Tampa Bay Buccaneers' : 'tam',
			'Tennessee Oilers' : 'oti',
			'Tennessee Titans' : 'oti',
			'Washington Redskins' : 'was'
	]

	CSVMerger() {

		cleanDir.mkdirs()

		(1980..2013).each {
			cleanGameData(it)
		}
	}

	def cleanGameData(def year) {

		File rawFile = new File(rawDir, "${year}.csv")
		File cleanFile = new File(cleanDir, "${year}.csv")

		def headers = ['week', 'date', 'game type', 'home', 'away', 'winner', 'loser']

		rawFile.withReader { reader ->

			List<String> rawHeaders = reader.readLine().split(',')
			List<String> lines = reader.readLines()

			cleanFile.withWriter { writer ->

				def gameType = 'season'
				writer.println headers.join(',')

				lines.each { it ->

					it = it.split(',')
					if (it[2] == 'Playoffs') {
						gameType = 'playoff'
					}
					else if (it[0] != 'Week') {

						def outLine = []

						def gameDate = Date.parse('MMMMM d yyyy', "${it[rawHeaders.indexOf('Date')]} $year")

						outLine << it[rawHeaders.indexOf('Week')]
						outLine << gameDate.format('E d MMM yyyy')
						if (gameType == 'playoff') {
							outLine << it[rawHeaders.indexOf('Week')]
							outLine[0] = '*'
						}
						else {
							outLine << gameType
						}

						String winner = it[rawHeaders.indexOf('Winner/tie')]
						String loser = it[rawHeaders.indexOf('Loser/tie')]

						if (it[rawHeaders.lastIndexOf('')] == '@') {
							outLine << loser
							outLine << winner

							// Make finding abbreviations easier
							if (!teamNamesToAbbr.containsKey(loser)) {
								println("$winner (${teamNamesToAbbr[winner]}) @ $loser (${teamNamesToAbbr[loser]}) " + gameDate.format('yyyyMMdd'))
							}
						}
						else {
							outLine << winner
							outLine << loser

							// Make finding abbreviations easier
							if (!teamNamesToAbbr.containsKey(winner)) {
								println("$loser (${teamNamesToAbbr[loser]}) @ $winner (${teamNamesToAbbr[winner]}) " + gameDate.format('yyyyMMdd'))
							}
						}
						outLine << winner
						outLine << loser

						writer.println outLine.join(',')
					}
				}
			}
		}
	}

	static def main(args) {
		new CSVMerger()
	}
}
