// 100 authors
FOREACH (i in range(1, 100) | CREATE (:author {id: i, name: 'author_' + i} ));


// 10 conferences for the last 5 years
FOREACH (i in range(1, 10) | 
	CREATE (c:conference {id: i, name: 'conference_' + i} )
	FOREACH (y in range(2016, 2019) | 
		CREATE (:edition {year: y}) -[:of]-> (c)
	)
);


// 10 journals with 5 volumes each
FOREACH (i in range(1, 10) | 
	CREATE (j:journal {id: i, name: 'journal_' + i} )
	FOREACH (v in range(1, 3) | 
		CREATE (:volume {number: v}) -[:of]-> (j)
	)
);


// 10 keywords
WITH ['data management', 'indexing', 'data modeling', 'big data',
	'data processing', 'data storage', 'data querying', 'management',
	'history', 'math'] AS keywords
FOREACH (i in range(1, 10) | CREATE (:keyword {id: i, name: keywords[i-1]} ));


// 10000 papers
FOREACH (i in range(1, 10000) | CREATE (p:paper {id: i, title: 'paper_' + i, year: 2016 + i%3} ));

// The first author takes the first 100 papers, the second takes papers 101-200, etc.
MATCH (p:paper), (a:author)
WHERE a.id = ceil(p.id/100.0)
CREATE (a) -[:writes]-> (p);

// For every paper, we take 3 different authors to be the reviewers.
MATCH (reviewer:author), (writer:author) -[:writes]-> (paper:paper)
WHERE reviewer.id <> writer.id
WITH collect(reviewer) as candidates, length(collect(reviewer)) as n_candidates, paper
WITH paper, candidates[toInt(rand()*(n_candidates - 1))] as r1, candidates[toInt(rand()*(n_candidates - 1))] as r2, candidates[toInt(rand()*(n_candidates - 1))] as r3
MERGE (r1) -[:reviews]-> (paper)
MERGE (r2) -[:reviews]-> (paper)
MERGE (r3) -[:reviews]-> (paper);

// A paper cites a random number of previous papers
MATCH (p1:paper), (p2:paper)
WHERE 0 < p1.id - p2.id <= 10 + toInt(rand()*20 - 10.0)
CREATE (p1) -[c:cites]-> (p2);

// A paper uses every keyword with a probability of 40% per keyword.
MATCH (p:paper), (k:keyword)
WITH p, k
WHERE rand() > 0.6
CREATE (p) -[r:related_to]-> (k);

// For the papers with an even id, they are assigned a random volume of a journal
MATCH (v:volume)
WITH collect(v) as volumes, length(collect(v)) as n_volumes
MATCH (p:paper)
WHERE p.id % 2 = 0
WITH p, volumes[toInt(rand()*(n_volumes - 1))] as selected_volume
CREATE (p) -[:published_in]-> (selected_volume);

// For the papers with an odd id, they are assigned a random edition of a conference
MATCH (e:edition)
WITH collect(e) as editions, length(collect(e)) as n_editions
MATCH (p:paper)
WHERE p.id % 2 = 1
WITH p, editions[toInt(rand()*(n_editions - 1))] as selected_edition
CREATE (p) -[:published_in]-> (selected_edition)
SET p.year = selected_edition.year;
