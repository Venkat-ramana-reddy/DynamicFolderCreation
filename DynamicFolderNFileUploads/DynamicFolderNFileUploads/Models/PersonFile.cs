using System;
using System.Collections.Generic;
using System.Linq;
using System.Web;
using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace DynamicFolderNFileUploads.Models
{
    public class PersonFile
    {
        [Key]
        public int FileID { get; set; }
        public string Name { get; set; }

        public string AdharCardPDF { get; set; }
        public string PanCard { get; set; }
        public string VoterCard { get; set; }

        [NotMapped]
        public HttpPostedFileBase Adhar { get; set; }
        [NotMapped]
        public HttpPostedFileBase Pan { get; set; }
        [NotMapped]
        public HttpPostedFileBase Voter { get; set; }
    }
}